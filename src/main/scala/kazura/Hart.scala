package kazura

import chisel3._
import chisel3.util.experimental.BoringUtils
import kazura.models.InstInfo
import kazura.modules.{BranchPredictor, RFWrite, ROB}
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
  val is_halt: Bool = Output(Bool())
}

class Hart(val im: Seq[UInt]) extends Module {
  val io: HartIO = IO(new HartIO)
  val m_bp: BranchPredictor = Module(new BranchPredictor())
  val s_if: IF = Module(new IF(im))
  val s_id: ID = Module(new ID)
  val s_ex: EX = Module(new EX)
  val s_im: IM = Module(new IM)
  val m_rob: ROB = Module(new ROB)

  val is_halt: Bool = RegInit(false.B)
  is_halt := is_halt | m_rob.io.commit_inst_info(0).ctrl.is_halt
  io.is_halt := is_halt

  val predict: Bool = m_bp.io.predict // 分岐予測器からの出力
  // --------------------
  // IF
  s_if.io.in.predict := predict
  s_if.io.in.predict_enable := s_id.io.inst_info.ctrl.is_branch
  s_if.io.in.predict_pc := s_id.io.jump_pc

  s_if.io.in.branch_mispredicted := s_ex.io.mispredicted
  s_if.io.in.branch_graduated := s_ex.io.inst_info_out.ctrl.is_branch // TODO: mispredictedを分岐時にのみ出すようにしたので消す
  s_if.io.in.restoration_pc := s_ex.io.restoration_pc_out

  s_if.io.in.is_jump := s_id.io.inst_info.ctrl.is_jump
  s_if.io.in.jump_pc := s_id.io.jump_pc

  s_if.io.in.stall := s_id.io.stall
  s_if.io.in.is_halt := is_halt

  // --------------------
  // BP
  m_bp.io.pc := s_if.io.out.pc
  m_bp.io.stall := s_id.io.stall
  m_bp.io.learning.valid := s_ex.io.inst_info_out.ctrl.is_branch
  m_bp.io.learning.bits.result := s_ex.io.alu_out
  m_bp.io.learning.bits.pc := s_ex.io.pc_out

  // --------------------
  // ID
  s_id.io.predict := predict // 分岐予測器未実装のため
  s_id.io.branch_mispredicted := s_ex.io.mispredicted
  s_id.io.branch_graduated := s_ex.io.inst_info_out.ctrl.is_branch
  s_id.io.if_out := s_if.io.out
  s_id.io.commit(0) := m_rob.io.commit(0)
  s_id.io.commit(1) := s_im.io.mem_out
  s_id.io.unreserved_head := m_rob.io.unreserved_head

  // --------------------
  // EX
  s_ex.io.predict := predict
  s_ex.io.inst_info := s_id.io.inst_info
  s_ex.io.source := s_id.io.source
  s_ex.io.rd := s_id.io.rd
  s_ex.io.next_pc := s_id.io.next_pc
  s_ex.io.branch_pc := s_id.io.jump_pc
  s_ex.io.pc := s_id.io.pc

  // --------------------
  // IM
  s_im.io.inst_info := m_rob.io.commit_inst_info(0)
  s_im.io.rob_out := m_rob.io.commit(0)
  s_im.io.rd_out := m_rob.io.commit_rd(0)

  // --------------------
  // ROB
  m_rob.io.used_num := s_id.io.used_num
  m_rob.io.graduate(0).valid := s_ex.io.inst_info_out.valid
  m_rob.io.graduate(0).bits.addr := s_ex.io.inst_info_out.rob_addr
  m_rob.io.graduate(0).bits.rd := s_ex.io.rd_out
  m_rob.io.graduate(0).bits.mispredicted := s_ex.io.mispredicted
  m_rob.io.graduate(0).bits.inst_info := s_ex.io.inst_info_out
  m_rob.io.graduate(0).bits.data := s_ex.io.alu_out
  // m_rob.io.graduate(1).valid := s_im.io.inst_info.valid
  // m_rob.io.graduate(1).bits.addr := s_im.io.inst_info.rob_addr
  // m_rob.io.graduate(1).bits.mispredicted := false.B
  // m_rob.io.graduate(1).bits.inst_info := s_im.io.inst_info
  // m_rob.io.graduate(1).bits.data := s_im.io.mem_out
  m_rob.io.graduate(1).valid := false.B
  m_rob.io.graduate(1).bits.addr := 0.U
  m_rob.io.graduate(1).bits.rd := 0.U
  m_rob.io.graduate(1).bits.mispredicted := false.B
  m_rob.io.graduate(1).bits.inst_info := InstInfo.nop
  m_rob.io.graduate(1).bits.data := 0.U

  // --------------------
  // IO
  io.pc := s_if.io.out.pc
  io.total_cnt := s_if.io.out.total_cnt
  io.rf := s_id.io.rf4debug

}
