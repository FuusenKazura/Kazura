package kazura.models

import chisel3._
import kazura.util.Params._

class Ctrl extends Bundle {
  val alu_op: UInt = UInt(ALUOP.NUM_W.W)
  val is_jump: Bool = Bool()
  val is_branch: Bool = Bool()
  val is_halt: Bool = Bool()
  val rf_w: Bool = Bool()
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
  val rs1_use: Bool = Bool()
  val rs2_use: Bool = Bool()
}
