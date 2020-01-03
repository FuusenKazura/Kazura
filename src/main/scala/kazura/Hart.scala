package kazura

import chisel3._
import kazura.modules.RFWrite
import kazura.util.Params._
import kazura.stages._

class HartIO extends Bundle

class Hart(val im: Seq[UInt]) extends Module {
  val io: HartIO = IO(new HartIO)
  val s_if: IF = Module(new IF(im))
  val s_id: ID = Module(new ID)
  val s_ex: EX = Module(new EX)

  // --------------------
  // IF
  s_if.io.in.predict := false.B
  s_if.io.in.predict_enable := s_id.io.ctrl.is_branch
  s_if.io.in.predict_pc := s_id.io.predict_pc
  // 分岐予測が失敗する条件は分岐条件がtrueの場合
  s_if.io.in.branch_mispredicted := s_ex.io.alu_ctrl_out.is_branch && s_ex.io.alu_out =/= 0.U
  s_if.io.in.branch_mispredicted_enable := s_ex.io.alu_ctrl_out.is_branch

  s_if.io.in.is_branch := s_ex.io.alu_ctrl_out.is_branch
  s_if.io.in.is_jump := s_ex.io.alu_ctrl_out.is_jump
  s_if.io.in.alu_out := s_ex.io.alu_out

  s_if.io.in.stall := s_id.io.stall

  // --------------------
  // ID
  val rfwrite: Vec[RFWrite] = Wire(Vec(RF.WRITE_PORT, new RFWrite))
  s_id.io.predict := false.B // 分岐予測器未実装のため
  s_id.io.branch_mispredicted := s_ex.io.alu_out
  s_id.io.branch_mispredicted_enable := s_ex.io.alu_ctrl_out.is_branch
  s_id.io.if_out := s_if.io.out
  rfwrite(0).rf_w := s_ex.io.ctrl.rf_w; rfwrite(0).rd_addr := s_ex.io.ctrl.rd_addr; rfwrite(0).data := s_ex.io.alu_out
  rfwrite(1).rf_w := false.B; rfwrite(1).rd_addr := 0.U; rfwrite(0).data := 0.U
  s_id.io.rf_write := rfwrite

  // --------------------
  // EX
  s_ex.io.ctrl := s_id.io.ctrl
  s_ex.io.source := s_id.io.source
}
