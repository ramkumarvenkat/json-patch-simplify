# json-patch-simplify
This is a small project that demonstrates how to remove redundant operations in the [json patch](https://workable.com/nr?l=http%3A%2F%2Fjsonpatch.com%2F)

## What does it do
The project takes in a json patch and tries to simplify the patch by removing redundant operations. It also transforms an operation based on the previous operation and deletes appropriate children if a parent path is deleted.

* `Remove` + `Add` can be combined into `Replace`
* `Add` can be removed if followed by a `Remove` (along with the `Remove` itself, since there is no point in removing a path that was added in a patch)
* `Add` or `Remove` can be removed if followed by a `Replace`
* `Replace` and `Remove` operations can be deduplicated
* If a `Remove` is done at a parent path, then all children without a `test`, `copy` or `move` (as the last operation) can be removed

## Algorithm
There are 2 data-structures:

* Operation tree: As each operation is processed in the patch, we build up the tree. Each node in the tree contains the node value (the path that ends here), the last operation that happened on the node and pointers to the children nodes.
* Result set: Contains the simplified operations. As each new operation is processed, the result set is updated to delete the previous operation as per the rules above (we actually do a soft delete and clean up the result set later)

## Code

[Simplifier.scala](https://github.com/ramkumarvenkat/json-patch-simplify/blob/master/src/main/scala/com/jsonpatch/simplify/Simplifier.scala) contains the complete code. Tests are [here](https://github.com/ramkumarvenkat/json-patch-simplify/tree/master/src/test/resources). The `input` and `output` folders contains the input patch and the simplified patch respectively.

* `1.json` tests `add`+`remove`
* `2.json` tests `add`+`remove` with everything negated
* `3.json` tests `add`+`remove` with the root path deleted
* `4.json` tests `parent`+`child` `removal` and `replace` substitution for a previous `remove`
* `5.json` tests more of the same, along with `remove` removal if followed by `replace` and deduplication of `replace`
* `6.json` has `test` and ensure none of the above rules can be applied, say if a `test` lies in between `remove` and `add`
* `7.json` tests `copy` and `move`
* `8.json` tests `copy`, `move`, `test`, `add` and `remove`
