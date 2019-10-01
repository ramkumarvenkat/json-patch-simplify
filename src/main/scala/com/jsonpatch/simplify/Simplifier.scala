package com.jsonpatch.simplify

import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.collection.mutable

class Simplifier {

  case class Operation(op: String, from: Option[String], path: String, value: Option[JsValue]) {
    def shouldRemovePreviousOperation(previousOpt: Option[Operation]) = {
      previousOpt.exists { previous =>
        op match {
          case "add" => previous.op.equals("remove") // Because we will transform add to replace
          case "remove" => previous.op.equals("add") || previous.op.equals("remove") || previous.op.equals("replace")
          case "replace" => previous.op.equals("add") || previous.op.equals("remove") || previous.op.equals("replace")
          case _ => false
        }
      }
    }

    def transformCurrentOperation(previousOpt: Option[Operation]) = {
      previousOpt.map { previous =>
        op match {
          case "add" if previous.op.equals("remove") => Some(this.copy(op = "replace"))
          case "remove" if previous.op.equals("add") => None //Previous operation will also be removed along with the current
          case _ => Some(this)
        }
      }.getOrElse(Some(this))
    }

    def ifParentOperationSupportsChildDeletion = op match {
      case "remove" => true
      case _ => false
    }

    def ifChildOperationSupportsDeletingItselfOnParentDeletion = op match {
      case "add" | "remove" | "replace" => true
      case _ => false
    }
  }
  case class WrappedOperation(operation: Operation, var deleted: Boolean = false)
  implicit val operationFormat = Json.format[Operation]

  case class Node(value: String, var lastOperation: Option[WrappedOperation], children: mutable.Map[String, Node])

  private def simplify(root: Node, operations: JsValue, result: mutable.ArrayDeque[WrappedOperation]) = {

    operations match {
      case JsArray(value) => value.foreach(v => parseAndModifyOperationTree(root, v.as[Operation], result))
      case o @ JsObject(_) => parseAndModifyOperationTree(root, Json.toJson(o).as[Operation], result)
      case _ =>
    }

    result
  }

  private def parseAndModifyOperationTree(root: Node, currentOperation: Operation, result: mutable.ArrayDeque[WrappedOperation]) = {
    currentOperation.op match {
      case "copy" | "move" =>
        val from = traverse(root, currentOperation.from.get)
        val to = traverse(root, currentOperation.path)
        to.children.addAll(from.children)
        parseAndTransform(from, currentOperation.from.get, currentOperation, result)
      case _ =>
        val n = traverse(root, currentOperation.path)
        parseAndTransform(n, currentOperation.path, currentOperation, result)
    }
  }

  private def parseAndTransform(n: Node, path: String, currentOperation: Operation, result: mutable.ArrayDeque[WrappedOperation]) = {
    removePreviousOperation(n, currentOperation)
    transformCurrentOperation(n, currentOperation, result)
    if(currentOperation.ifParentOperationSupportsChildDeletion) n.children.foreach(entry => deletePossibleChildren(n, entry._2, result))
  }

  private def removePreviousOperation(n: Node, currentOperation: Operation) =
    if(n.lastOperation.filter(_.deleted == false).nonEmpty && currentOperation.shouldRemovePreviousOperation(n.lastOperation.map(_.operation)))
      n.lastOperation.foreach(op => op.deleted = true)

  private def transformCurrentOperation(n: Node, currentOperation: Operation, result: mutable.ArrayDeque[WrappedOperation]) =
    currentOperation.transformCurrentOperation(n.lastOperation.map(_.operation)).foreach { op =>
      val newOp = WrappedOperation(op)
      result.append(newOp)
      n.lastOperation = Some(newOp)
  }

  private def deletePossibleChildren(parent: Node, child: Node, result: mutable.ArrayDeque[WrappedOperation]): Unit = {
    if(child.lastOperation.filter(_.operation.ifChildOperationSupportsDeletingItselfOnParentDeletion).nonEmpty)
      child.lastOperation.foreach(op => op.deleted = true)

    child.children.foreach(entry => deletePossibleChildren(child, entry._2, result))
  }

  private def traverse(root: Node, op: String): Node = {
    val tokens = op.split("/")
    var node = root
    for(i <- 1 until tokens.length) {
      val child = node.children.getOrElse(tokens(i), Node(tokens(i), None, mutable.Map.empty))
      node.children.addOne(tokens(i), child)
      node = child
    }
    node
  }

  def simplify(operations: String): JsValue = {
    val root = Node("/", None, mutable.Map.empty)

    val result: mutable.ArrayDeque[WrappedOperation] = mutable.ArrayDeque.empty[WrappedOperation]
    simplify(root, Json.parse(operations), result)

    Json.toJson(result.filter(_.deleted == false).map(_.operation))
  }
}
