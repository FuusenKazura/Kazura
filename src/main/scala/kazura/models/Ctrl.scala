package kazura.models

import chisel3._
import kazura.models.Inst.Nop
import kazura.util.Params._

class Ctrl extends Bundle {
  val alu_op: UInt = UInt(ALUOP.NUM_W.W)
  val is_jump: Bool = Bool()
  val is_branch: Bool = Bool()
  val rf_w: Bool = Bool()
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
  val rs1_use: Bool = Bool()
  val rs2_use: Bool = Bool()
}

object Ctrl {
  def nop: Ctrl = {
    val n = Wire(new Ctrl)
    n.alu_op := Nop.alu_op
    n.is_jump := Nop.is_jump
    n.is_branch := Nop.is_branch
    n.rf_w := Nop.rf_w
    n.mem_r := Nop.mem_r
    n.mem_w := Nop.mem_w
    n.rs1_use := false.B
    n.rs2_use := false.B
    n
  }
}
