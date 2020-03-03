package statement

import Visitor
import identifier.Identifier

class ReadStatement(val id: Identifier) : Statement() {

    override fun accept(v: Visitor) {
        if(!v.preVisit(this)) return
        id.accept(v)
        v.postVisit(this)
    }

    override fun toString(): String {
        return "READ $id"
    }
}