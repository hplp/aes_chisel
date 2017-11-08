package lfsr

import chisel3.iotesters
import chisel3.iotesters.{ ChiselFlatSpec, Driver, PeekPokeTester }

class LFSRUnitTester(c: LFSR) extends PeekPokeTester(c) {
  //init D0123456 to 1
  var D0123456 = 1
  for (t <- 0 until 63) {
    step(1)
    val bit = ((D0123456 >> 5) ^ (D0123456 >> 4)) & 1;
    D0123456 = (D0123456 << 1) | bit;
    if (D0123456 > 63) {
      D0123456 = D0123456 - 64
    }
    expect(c.io.lfsr_6, D0123456)
  }
}

class LFSRTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for (backendName <- backendNames) {
    "LFSR" should s"generate random numbers (with ${backendName})" in {
      Driver(() => new LFSR, backendName) {
        c => new LFSRUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in the tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new LFSR) {
      c => new LFSRUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from the test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new LFSR) {
      c => new LFSRUnitTester(c)
    } should be(true)
  }
}
