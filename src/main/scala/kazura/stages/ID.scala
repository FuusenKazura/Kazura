package kazura.stages

import chisel3._
import chisel3.util.MuxLookup
import kazura.modules.{BusyBit, Ctrl, Decoder, RFWrite, RegisterFile}
import kazura.util.Params
import kazura.util.Params._

class IDIO extends Bundle {
  val branch_graduated: Bool = Input(Bool())
  val branch_mispredicted: Bool = Input(Bool())
  val if_out: IFOut = Input(new IFOut)
  val rf_write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val prev_stall: Bool = Input(Bool())
  val next_pc: UInt = Output(UInt(LEN.W))

  val ctrl: Ctrl = Output(new Ctrl)
  val source: Vec[UInt] = Output(Vec(RF.READ_PORT, UInt(LEN.W)))
  val stall: Bool = Output(Bool())
}

class ID extends Module {
  val io: IDIO = IO(new IDIO)

  // stallが起きると同じデータで再試行
  val if_out: IFOut = Mux(io.prev_stall, RegNext(if_out), io.if_out)
  // 書き込みデータはstall関係なし
  val rf_write: Vec[RFWrite] = io.rf_write

  val busy_bit: BusyBit = Module(new BusyBit)
  busy_bit.io.release := rf_write
  busy_bit.io.req_rs_addr(0) := if_out.inst_bits.rd
  busy_bit.io.req_rs_addr(1) := if_out.inst_bits.rs
  busy_bit.io.req_rd_addr := if_out.inst_bits.rd

  val decoder: Decoder = Module(new Decoder)
  decoder.io.inst_bits := if_out.inst_bits

  val reg_file: RegisterFile = Module(new RegisterFile)
  reg_file.io.read_addr(0) := if_out.inst_bits.rd
  reg_file.io.read_addr(1) := if_out.inst_bits.rs
  reg_file.io.write := rf_write

  // branch, jump命令の次の命令は無効化
  val clear_instruction: Bool = RegInit(false.B)
  when (decoder.io.ctrl.is_jump || decoder.io.ctrl.is_branch) {
    clear_instruction := true.B
  } .otherwise {
    clear_instruction := false.B
  }

  // すべてのレジスタが準備できない時はhalt
  val operands_available: Bool = !(
    (busy_bit.io.rs_available(0) || !decoder.io.ctrl.rs1_use) &&
    (busy_bit.io.rs_available(1) || !decoder.io.ctrl.rs2_use)
  )

  val stall: Bool = !operands_available
  io.stall := RegNext(stall, false.B)

  // stall時の命令は無効化
  val ctrl: Ctrl = Mux(stall || clear_instruction,
    0.U.asTypeOf(new Ctrl),
    decoder.io.ctrl)
  io.ctrl := RegNext(ctrl)

  io.next_pc := RegNext(io.if_out.pc + Mux(decoder.io.ctrl.is_jump,
    io.if_out.inst_bits.imm9s, io.if_out.inst_bits.disp6s))

  io.source(0) := RegNext(MuxLookup(0.U, decoder.io.source_sel(0), Seq(
    Source1.DISP6U -> if_out.inst_bits.disp6u,
    Source1.RD -> reg_file.io.out(0),
    Source1.ZERO -> 0.U
  )))
  io.source(1) := RegNext(MuxLookup(0.U, decoder.io.source_sel(1), Seq(
    Source2.IMM9S -> if_out.inst_bits.imm9s,
    Source2.RS -> reg_file.io.out(1),
    Source2.ONE -> 1.U
  )))
}
