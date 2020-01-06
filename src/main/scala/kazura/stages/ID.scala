package kazura.stages

import chisel3._
import chisel3.util.MuxLookup
import kazura.modules.{BusyBit, Ctrl, Decoder, RFWrite, RegisterFile}
import kazura.util.Params._
import kazura.models.Inst

class IDIO extends Bundle {
  val predict: Bool = Input(Bool()) // 分岐予測の予測
  val branch_mispredicted: Bool = Input(Bool()) // 分岐予測の予測を失敗したか
  val branch_graduated: Bool = Input(Bool()) // 分岐命令の演算が完了したか(完了した演算が分岐であるか)

  val if_out: IFOut = Input(new IFOut)
  val rf_write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))

  val next_pc: UInt = Output(UInt(LEN.W))
  val ctrl: Ctrl = Output(new Ctrl)
  val source: Vec[UInt] = Output(Vec(RF.READ_PORT, UInt(LEN.W)))
  val stall: Bool = Output(Bool())

  val rf4debug: Vec[UInt] = Vec(RF.NUM, Output(UInt(LEN.W)))
}

class ID extends Module {
  val io: IDIO = IO(new IDIO)
  // stallが起きていたら同じデータで再試行
  // MEMO: ID以前からくるデータの場合stallに影響され、再試行が必要
  // MEMO: 循環参照を解決するために必要
  val if_out: IFOut = Wire(new IFOut)
  val if_out_r: IFOut = RegNext(if_out)
  if_out := Mux(io.stall, if_out_r, io.if_out)
  // stallが起きていたら同じデータで再試行
  val predict: Bool = Wire(Bool())
  val predict_r: Bool = RegNext(predict)
  predict := Mux(io.stall, predict_r, io.predict)

  // 書き込みデータはIDより後の話なのでstall関係なし
  val rf_write: Vec[RFWrite] = io.rf_write

  val stall: Bool = Wire(Bool())

  val decoder: Decoder = Module(new Decoder)
  decoder.io.inst_bits := if_out.inst_bits

  val reg_file: RegisterFile = Module(new RegisterFile)
  reg_file.io.read_addr(0) := if_out.inst_bits.rd
  reg_file.io.read_addr(1) := if_out.inst_bits.rs
  reg_file.io.write := rf_write

  val busy_bit: BusyBit = Module(new BusyBit)
  busy_bit.io.branch_mispredicted := io.branch_mispredicted
  busy_bit.io.branch_graduated := io.branch_graduated
  busy_bit.io.release := rf_write
  busy_bit.io.req_rs_addr(0) := if_out.inst_bits.rd
  busy_bit.io.req_rs_addr(1) := if_out.inst_bits.rs
  // stallする命令ではrdを確保しない
  val clear_instruction: Bool = Wire(Bool())
  busy_bit.io.req_rd_w := !clear_instruction && !stall && decoder.io.ctrl.rf_w
  busy_bit.io.req_rd_addr := if_out.inst_bits.rd

  // # clear_instruction
  // - mispredictedの次
  // - pcが変更された次に来る命令を無効化したい:
  //    + jumpの次
  //    + 分岐したとき(branch次にpredictがtrueの時)
  when (
       (io.branch_graduated && io.branch_mispredicted)
    || (RegNext(decoder.io.ctrl.is_jump, false.B))
    || (RegNext(decoder.io.ctrl.is_branch && predict, false.B))
  ) {
    clear_instruction := true.B
  } .otherwise {
    clear_instruction := false.B
  }

  // 分岐命令の発行中は次の命令の発行を停止する
  // Speculative execution beyond branchは一旦諦めよう……
  // TODO: このロジックバグってる(連続でbranchが来る場合)
  val branch_pending: Bool = RegInit(false.B)
  branch_pending := Mux(io.branch_graduated,
    false.B,
    (!stall && decoder.io.ctrl.is_branch) || branch_pending)

  // すべてのレジスタが準備できない時はhalt
  val operands_available: Bool =
    (busy_bit.io.rs_available(0) || !decoder.io.ctrl.rs1_use) &&
    (busy_bit.io.rs_available(1) || !decoder.io.ctrl.rs2_use)

  stall := !operands_available || branch_pending
  io.stall := RegNext(
    !(io.branch_graduated && io.branch_mispredicted) // 分岐予測に失敗した場合、状態をリセット
      && stall,
    false.B)

  io.ctrl := RegNext(
    Mux(stall || clear_instruction, Inst.nop, decoder.io.ctrl),
    Inst.nop
  )

  io.next_pc := RegNext(Mux(decoder.io.ctrl.is_jump,
    if_out.pc + if_out.inst_bits.imm9s, if_out.pc + if_out.inst_bits.disp6s))

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
  // printf("cnt: %d, pc: %d, op: %d\n", if_out.total_cnt, if_out.pc, if_out.inst_bits.op)
  printf("branch_mispredicted_enable: %d, branch_mispredicted: %d\n", io.branch_graduated, io.branch_mispredicted)
  printf("stall: %d, !operands_avail: %d, branch_pend: %d\n", stall, !operands_available, branch_pending)
  // printf("alu_op: %d\n", decoder.io.ctrl.alu_op)
  printf("source(0): %d\n", io.source(0))
  printf("source(1): %d\n", io.source(1))
  printf("next_addr: %d\n", io.next_pc)
  printf("----------\n")

  io.rf4debug := reg_file.io.rf4debug
}
