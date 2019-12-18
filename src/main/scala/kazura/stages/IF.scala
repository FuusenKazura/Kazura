package kazura.stages

import chisel3._
import kazura.modules._

object Main extends App {
  Driver.execute(args, () => new Fetch)
}

// IF Stage
// 現段階では分岐予測等積んでいないのでFetchと等価
class IF extends Module {
  val io = IO(new IFIO)

  val fetch = Module(new Fetch)
  io <> fetch.io
}
