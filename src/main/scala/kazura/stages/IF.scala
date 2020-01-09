package kazura.stages

import chisel3._
import kazura.models.InstBits
import kazura.modules._
import kazura.util.Params._

class IFIn extends Bundle {
  val branch_mispredicted: Bool = Bool() // 分岐予測の予測を失敗したか
  val branch_graduated: Bool = Bool() // 分岐命令の演算が完了したか(完了した演算が分岐であるか)
  val restoration_pc: UInt = UInt(LEN.W)

  val predict: Bool = Bool() // 分岐予測の予測
  val predict_enable: Bool = Bool()
  val predict_pc: UInt = UInt(LEN.W)

  val is_jump: Bool = Bool()
  val jump_pc: UInt = UInt(LEN.W)

  val stall: Bool = Bool()
}
class IFOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
  val inst_bits: InstBits = new InstBits
}
class IFIO extends Bundle {
  val in = Input(new IFIn)
  val out = Output(new IFOut)
}

class IF(val im: Seq[UInt] = (0 until 256).map(_.U)) extends Module {
  val io = IO(new IFIO)
  val inst_mem: Vec[UInt] = RegInit(VecInit(im))
  val pc: UInt = RegInit(0.U(LEN.W))
  val total_cnt: UInt = RegInit(0.U(LEN.W))
  total_cnt := total_cnt + 1.U(LEN.W)

  val fetch: Fetch = Module(new Fetch)
  fetch.io.in.predict := io.in.predict
  fetch.io.in.predict_enable := io.in.predict_enable
  fetch.io.in.predict_pc := io.in.predict_pc
  fetch.io.in.branch_mispredicted := io.in.branch_mispredicted
  fetch.io.in.branch_graduated := io.in.branch_graduated
  fetch.io.in.restoration_pc := io.in.restoration_pc

  fetch.io.in.is_jump := io.in.is_jump
  fetch.io.in.jump_pc := io.in.jump_pc
  fetch.io.in.stall := io.in.stall

  fetch.io.prev_pc := pc

  pc := fetch.io.out.pc

  io.out.pc := pc
  io.out.total_cnt := total_cnt
  io.out.inst_bits := inst_mem(pc).asTypeOf(new InstBits)
}
