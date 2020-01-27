package kazura.stages

import chisel3.{Mux, _}
import chisel3.util.{MuxLookup, Valid, log2Ceil}
import kazura.modules._
import kazura.util.Params._
import kazura.models._

class IDIO extends Bundle {
  val predict: Bool = Input(Bool()) // 分岐予測の予測
  val branch_mispredicted: Bool = Input(Bool()) // 分岐予測の予測を失敗したか
  val branch_graduated: Bool = Input(Bool()) // 分岐命令の演算が完了したか(完了した演算が分岐であるか)

  val if_out: IFOut = Input(new IFOut)

  val commit: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val unreserved_head: Vec[Valid[UInt]] = Input(Vec(PARALLEL, Valid(UInt(log2Ceil(ROB.BUF_SIZE).W))))
  val used_num: UInt = Output(UInt(log2Ceil(PARALLEL + 1).W))

  val jump_pc: UInt = Output(UInt(LEN.W))
  val next_pc: UInt = Output(UInt(LEN.W))
  val inst_info: InstInfo = Output(new InstInfo)
  val source: Vec[UInt] = Output(Vec(RF.READ_PORT, UInt(LEN.W)))
  val rd: UInt = Output(UInt(LEN.W))
  val stall: Bool = Output(Bool())
  val pc: UInt = Output(UInt(LEN.W))

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
  val rf_write: Vec[RFWrite] = io.commit

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
  busy_bit.io.req_rd_w := !clear_instruction && !stall && (decoder.io.ctrl.rf_w || decoder.io.ctrl.mem_r)
  busy_bit.io.req_rd_addr := if_out.inst_bits.rd

  // # clear_instruction
  // - mispredictedの次
  // - pcが変更された次に来る命令を無効化したい:
  //    + jumpの次
  //    + 分岐したとき(branch次にpredictがtrueの時)
  when (
       io.branch_mispredicted
    || (RegNext(!clear_instruction && decoder.io.ctrl.is_jump, false.B))
    || (RegNext(!clear_instruction && decoder.io.ctrl.is_branch, false.B) && predict)
  ) {
    clear_instruction := true.B
  } .otherwise {
    clear_instruction := false.B
  }

  // すべてのレジスタが準備できない時はstall
  val operands_available: Bool =
    (busy_bit.io.rs_available(0) || !decoder.io.ctrl.rs1_use) &&
    (busy_bit.io.rs_available(1) || !decoder.io.ctrl.rs2_use)

  // ROBが確保できなかった場合はstall
  val rob_available: Vec[Bool] = Wire(Vec(PARALLEL, Bool()))
  rob_available(0) := io.unreserved_head(0).valid
  for (i <- 1 until PARALLEL) {
    rob_available(i) := rob_available(i - 1) && io.unreserved_head(i).valid
  }

  // clear_instructionsが有効なときはstallしない
  stall := Mux(clear_instruction, false.B, !operands_available || !rob_available(0))
  io.stall := RegNext(stall, false.B)

  // NOPの場合はreserveしないようにする
  io.used_num := RegNext(Mux(
    stall || clear_instruction || if_out.inst_bits.op === Inst.Nop.op,
    0.U,
    1.U
  ), 0.U)

  io.inst_info.valid := RegNext(
    !(stall || clear_instruction || if_out.inst_bits.op === Inst.Nop.op),
    InstInfo.nop.valid
  )
  io.inst_info.pc := RegNext(if_out.pc, InstInfo.nop.pc)
  io.inst_info.total_cnt := RegNext(if_out.total_cnt, InstInfo.nop.total_cnt)
  io.inst_info.rd_addr := RegNext(if_out.inst_bits.rd, InstInfo.nop.rd_addr)
  io.inst_info.rob_addr := io.unreserved_head(0).bits
  io.inst_info.ctrl := RegNext(
    Mux(stall || clear_instruction ,InstInfo.nop.ctrl, decoder.io.ctrl),
    InstInfo.nop.ctrl
  )

  io.jump_pc := RegNext(Mux(decoder.io.ctrl.is_jump,
    if_out.pc + if_out.inst_bits.imm9s, if_out.pc + if_out.inst_bits.disp6s))

  io.next_pc := RegNext(if_out.pc + 1.U)

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
  io.rd := RegNext(reg_file.io.out(0))
  io.pc := RegNext(if_out.pc)
  // printf("cnt: %d, pc: %d, op: %d\n", if_out.total_cnt, if_out.pc, if_out.inst_bits.op)
  // printf("branch_mispredicted_enable: %d, branch_mispredicted: %d\n", io.branch_graduated, io.branch_mispredicted)
  // printf("stall: %d, !operands_avail: %d, !rob_avail(0): %d\n", stall, !operands_available, !rob_available(0))
  // printf("alu_op: %d\n", decoder.io.ctrl.alu_op)
  // printf("source(0): %d\n", io.source(0))
  // printf("source(1): %d\n", io.source(1))
  // printf("next_addr: %d\n", io.jump_pc)
  // printf("rf: ")
  // for (i <- reg_file.io.rf4debug.indices) printf("%d, ", reg_file.io.rf4debug(i))
  // printf("\n")
  // printf("----------\n")

  io.rf4debug := reg_file.io.rf4debug
}
