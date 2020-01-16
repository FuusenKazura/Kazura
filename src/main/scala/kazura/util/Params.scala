package kazura.util

import chisel3.util._

object Params {
  val LEN = 16 // 命令長
  val LEN_W = log2Ceil(LEN) // Log2(LEN)
  val PARALLEL = 4

  object RF {
    val NUM = 8
    val NUM_W = log2Ceil(NUM)

    val WRITE_PORT = 2
    val READ_PORT = 2
  }
  object Source1 {
    val ZERO = 0
    val RD = 1
    val DISP6U = 3

    val NUM = 3
    val NUM_W = log2Ceil(NUM)
  }
  object Source2 {
    val ONE = 0
    val RS = 1
    val IMM9S = 2

    val NUM = 3
    val NUM_W = log2Ceil(NUM)
  }
  object ALUOP {
    val ADD = 0
    val SUB = 1
    val AND = 2
    val OR = 3
    val BEQ = 4
    val BGT = 5

    val NUM = 6
    val NUM_W = log2Ceil(NUM)
  }
  object COND_TYPE {
    val NOP = 0
    val NO_CONDITIONAL = 1
    val EQ = 2
    val GT = 3

    val NUM = 4
    val NUM_W = log2Ceil(NUM)
  }

  object MEM {
    val NUM: Int = 128
  }
  object ROB {
    val BUF_SIZE: Int = 16
    val RESERVE_SIZE: Int = 2
  }
}
