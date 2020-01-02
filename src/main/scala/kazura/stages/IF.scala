package kazura.stages

import chisel3._
import kazura.models.InstBits
import kazura.modules._
import kazura.util.Params._

object Main extends App {
  Driver.execute(args, () => new Fetch)
}
class IFIn extends Bundle {
  val predict: Bool = Bool() // 分岐予測の予測
  val predict_enable: Bool = Bool()
  val predict_pc: UInt = UInt(LEN.W)
  val branch_mispredicted: Bool = Bool() // 分岐予測の予測を失敗したか
  val branch_mispredicted_enable: Bool = Bool() // 分岐命令の演算が完了したか(完了した演算が分岐であるか)

  val is_branch: Bool = Bool()
  val is_jump: Bool = Bool()
  val alu_out: UInt = UInt(LEN.W)
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

  val fetch: Fetch = Module(new Fetch)
  fetch.io.in.prev_pc := pc
  fetch.io.in.prev_total_cnt := total_cnt
  fetch.io.in <> io.in

  pc := fetch.io.out.pc
  total_cnt := fetch.io.out.total_cnt

  io.out.pc := pc
  io.out.total_cnt := total_cnt
  io.out.inst_bits := inst_mem(pc)
}
