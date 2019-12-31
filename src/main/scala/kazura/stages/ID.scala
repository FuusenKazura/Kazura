package kazura.stages

import chisel3._
import chisel3.util.MuxLookup
import kazura.modules.{BusyBit, Ctrl, Decoder, RFWrite, RegisterFile}
import kazura.util.Params
import kazura.util.Params._

class IDIO extends Bundle {
  val predict: Bool = Input(Bool()) // 分岐予測の予測
  val branch_end: Bool = Input(Bool()) // 分岐命令の演算が完了したか(完了した演算が分岐であるか)
  val branch_mispredicted: Bool = Input(Bool()) // 分岐予測の予測を失敗したか

  val if_out: IFOut = Input(new IFOut)
  val rf_write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))

  val next_pc: UInt = Output(UInt(LEN.W))
  val ctrl: Ctrl = Output(new Ctrl)
  val source: Vec[UInt] = Output(Vec(RF.READ_PORT, UInt(LEN.W)))
  val stall: Bool = Output(Bool())
}

class ID extends Module {
  val io: IDIO = IO(new IDIO)
  // stallが起きていたら同じデータで再試行
  // MEMO: ID以前からくるデータの場合stallに影響され、再試行が必要
  val if_out: IFOut = Mux(io.stall, RegNext(if_out), io.if_out)
  // stallが起きていたら同じデータで再試行
  val predict: Bool = Mux(io.stall, RegNext(predict), io.predict)

  // 書き込みデータはIDより後の話なのでstall関係なし
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

  // - mispredictedの次
  // - pcが変更された次に来る命令を無効化したい:
  //    + jumpの次
  //    + 分岐したとき(branch次にpredictがtrueの時)
  val clear_instruction: Bool = Wire(Bool())
  when (
       (io.branch_end && io.branch_mispredicted)
    || (RegNext(decoder.io.ctrl.is_jump, false.B))
    || (RegNext(decoder.io.ctrl.is_branch && predict, false.B))
  ) {
    clear_instruction := true.B
  } .otherwise {
    clear_instruction := false.B
  }

  // 分岐命令の発行中は次の命令の発行を停止する
  // Speculative execution beyond branchは一旦諦めよう……
  val branch_pending: Bool = RegInit(false.B)
  when (io.branch_end) {
    branch_pending := false.B
  } .otherwise {
    branch_pending := decoder.io.ctrl.is_branch || branch_pending
  }

  // すべてのレジスタが準備できない時はhalt
  val operands_available: Bool = !(
    (busy_bit.io.rs_available(0) || !decoder.io.ctrl.rs1_use) &&
    (busy_bit.io.rs_available(1) || !decoder.io.ctrl.rs2_use)
  )

  val stall: Bool = !operands_available || branch_pending

  io.stall := RegNext(stall, false.B)

  io.ctrl := RegNext(
    Mux(stall || clear_instruction, 0.U.asTypeOf(new Ctrl), decoder.io.ctrl),
    0.U.asTypeOf(new Ctrl)
  )

  io.next_pc := RegNext(io.if_out.pc + Mux(decoder.io.ctrl.is_jump,
    io.if_out.inst_bits.imm9s, io.if_out.inst_bits.disp6s))

  io.source(0) := RegNext(MuxLookup(decoder.io.source_sel(0), 0.U, Seq(
    Source1.DISP6U.U -> if_out.inst_bits.disp6u,
    Source1.RD.U -> reg_file.io.out(0),
    Source1.ZERO.U -> 0.U
  )))
  io.source(1) := RegNext(MuxLookup(decoder.io.source_sel(1), 0.U, Seq(
    Source2.IMM9S.U -> if_out.inst_bits.imm9s,
    Source2.RS.U -> reg_file.io.out(1),
    Source2.ONE.U -> 1.U
  )))
}
