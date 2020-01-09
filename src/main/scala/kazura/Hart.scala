package kazura

import chisel3._
import chisel3.util.experimental.BoringUtils
import kazura.modules.RFWrite
import kazura.util.Params._
import kazura.stages._

object Main {
  val prog = Seq(
    "b1001_001_000000000".U,  // 0: $1 = 0
    "b1001_010_000000000".U,  // 1: $2 = 0
    "b1001_011_000001001".U,  // 2: $3 = 9
    "b1001_100_000000000".U,  // 3: $4 = 0
    "b0101_001_000000001".U,  // 4: $1 += 1
    "b0001_010_001_000000".U, // 5: $2 += $1
    "b1101_001_011_000011".U, // 6: if($1 > $3) pc+= 3
    "h0000".U,                // 7: NOP
    "b1110_000_111111100".U,  // 7: pc = pc - 3 - 1
    "b0001_100_010_000000".U  // 8: $4 += $2
  )
  def main(args: Array[String]): Unit = {
    chisel3.Driver.execute(args, () => new Hart(
      prog ++ Seq.fill(32 - prog.length)("h0000".U)))
  }
}

class HartIO extends Bundle {
  val pc: UInt = Output(UInt(LEN.W))
  val total_cnt: UInt = Output(UInt(LEN.W))
  val rf: Vec[UInt] = Output(Vec(RF.NUM, UInt(LEN.W)))
}

class Hart(val im: Seq[UInt]) extends Module {
  val io: HartIO = IO(new HartIO)
  val s_if: IF = Module(new IF(im))
  val s_id: ID = Module(new ID)
  val s_ex: EX = Module(new EX)
  val s_im: IM = Module(new IM)

  // --------------------
  // IF
  s_if.io.in.predict := false.B
  s_if.io.in.predict_enable := false.B
  s_if.io.in.predict_pc := "hFFFF".U
  // 分岐予測が失敗する条件は分岐条件がtrueの場合
  s_if.io.in.branch_mispredicted := s_ex.io.alu_out =/= 0.U
  s_if.io.in.branch_mispredicted_enable := s_ex.io.alu_ctrl_out.is_branch
  s_if.io.in.restoration_pc := s_ex.io.restoration_pc_out

  s_if.io.in.is_branch := s_ex.io.alu_ctrl_out.is_branch
  s_if.io.in.is_jump := s_id.io.ctrl.is_jump
  s_if.io.in.jump_pc := s_id.io.next_pc
  s_if.io.in.alu_out := s_ex.io.alu_out

  s_if.io.in.stall := s_id.io.stall

  // --------------------
  // ID
  val rfwrite: Vec[RFWrite] = Wire(Vec(RF.WRITE_PORT, new RFWrite))
  s_id.io.predict := false.B // 分岐予測器未実装のため
  s_id.io.branch_mispredicted := s_ex.io.alu_out =/= 0.U
  s_id.io.branch_graduated := s_ex.io.alu_ctrl_out.is_branch
  s_id.io.if_out := s_if.io.out
  rfwrite(0).rf_w := s_ex.io.alu_ctrl_out.rf_w; rfwrite(0).rd_addr := s_ex.io.alu_ctrl_out.rd_addr; rfwrite(0).data := s_ex.io.alu_out
  rfwrite(1).rf_w := s_im.io.out.valid;         rfwrite(1).rd_addr := s_im.io.out.bits.addr;        rfwrite(1).data := s_im.io.out.bits.data
  s_id.io.rf_write := rfwrite

  // --------------------
  // EX
  s_ex.io.ctrl := s_id.io.ctrl
  s_ex.io.source := s_id.io.source
  s_ex.io.rd := s_id.io.rd
  s_ex.io.restoration_pc := s_id.io.next_pc // BPが常にfalseなので

  // --------------------
  // IM
  s_im.io.rd_addr := s_ex.io.alu_ctrl_out.rd_addr
  s_im.io.write.valid := s_ex.io.alu_ctrl_out.mem_w
  s_im.io.write.bits.addr := s_ex.io.alu_out
  s_im.io.write.bits.data := s_ex.io.rd_out
  s_im.io.read.valid := s_ex.io.ctrl.mem_r
  s_im.io.read.bits := s_ex.io.alu_out

  // --------------------
  // IO
  io.pc := s_if.io.out.pc
  io.total_cnt := s_if.io.out.total_cnt
  io.rf := s_id.io.rf4debug
}
