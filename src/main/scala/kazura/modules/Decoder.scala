package kazura.modules

import chisel3._
import kazura.models.Inst
import kazura.util.Params._

class IDIO extends Bundle {
  val inst: Inst = Input(new Inst)
  val ctrl: Ctrl = Output(new Ctrl)
}

class Ctrl extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val alu_op: UInt = UInt(2.W)
  val cond_type: UInt = UInt(2.W)
  val rf_r: Bool = Bool()
  val rf_w: Bool = Bool()
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
  val pc_w: Bool = Bool()
  val source: Vec[UInt] = Vec(2, UInt(LEN.W))
}

class Decoder extends Module {
  val io: IDIO = IO(new IDIO)
}
