package kazura.stages

import chisel3._
import chisel3.util.MuxLookup
import kazura.modules.{BusyBit, Ctrl, Decoder, RFWrite, RegisterFile}
import kazura.util.Params
import kazura.util.Params._

class IDIO extends Bundle {
  val if_out: IFOut = Input(new IFOut)
  val rf_write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val prev_stall: Bool = Input(Bool())

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

  // すべてのレジスタが準備できない時はhalt
  val current_stall: Bool = !(busy_bit.io.rd_available && busy_bit.io.rs_available.forall((a:Bool) => a))
  io.stall := RegNext(current_stall)

  // stall時の命令は無効化
  when (!current_stall) {
    io.ctrl := RegNext(decoder.io.ctrl)
  } .otherwise {
    io.ctrl := RegNext(DontCare)
    io.ctrl.mem_w := RegNext(false.B)
    io.ctrl.pc_w := RegNext(false.B)
    io.ctrl.rf_w := RegNext(false.B)
  }

  io.source(0) := RegNext(MuxLookup(0.U, decoder.io.source_sel(0), Seq(
    Source1.PC -> if_out.pc,
    Source1.DISP6U -> if_out.inst_bits.disp6u,
    Source1.RD -> reg_file.io.out(0),
    Source1.ZERO -> 0.U
  )))
  io.source(1) := RegNext(MuxLookup(0.U, decoder.io.source_sel(1), Seq(
    Source2.IMM9S -> if_out.inst_bits.imm9s,
    Source2.DISP6S -> if_out.inst_bits.disp6s,
    Source2.RS -> reg_file.io.out(1),
    Source2.ONE -> 1.U
  )))
}
