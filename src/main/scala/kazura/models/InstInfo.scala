package kazura.models

import chisel3._
import chisel3.util._
import kazura.models.Inst.Nop
import kazura.util.Params._

class InstInfo extends Bundle {
  val valid: Bool = Bool()
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val rob_addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W)
  val ctrl: Ctrl = new Ctrl
}

object InstInfo {
  def nop: InstInfo = {
    val n = Wire(new InstInfo)
    n.pc := 65535.U
    n.total_cnt := 65535.U
    n.valid := false.B
    n.rd_addr := 0.U
    n.rob_addr := 0.U
    n.ctrl := nop_ctrl
    n
  }
  private def nop_ctrl: Ctrl = {
    val n = Wire(new Ctrl)
    n.alu_op := Nop.alu_op
    n.is_jump := Nop.is_jump
    n.is_branch := Nop.is_branch
    n.is_halt := Nop.is_halt
    n.rf_w := Nop.rf_w
    n.mem_r := Nop.mem_r
    n.mem_w := Nop.mem_w
    n.rs1_use := false.B
    n.rs2_use := false.B
    n
  }
}
