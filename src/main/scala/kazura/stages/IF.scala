package kazura.stages

import chisel3._
import kazura.modules._

// IF Stage
// 現段階では分岐予測等積んでいないのでFetchと等価
class IF extends Module {
  val io = IO(new IFIO)

  val fetch = Module(new Fetch)
  io <> fetch.io
}
