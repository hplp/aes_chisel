package aes


import chisel3.iotesters
import chisel3.iotesters.{ ChiselFlatSpec, Driver, PeekPokeTester }

class SubBytesUnitTester (c: SubBytes) extends PeekPokeTester(c) {
}

class SubBytesTester extends ChiselFlatSpec {
  private val backendNames = Array[String]("firrtl", "verilator")
  for (backendName <- backendNames) {
    "SubBytes" should s"execute AES SubBytes (with ${backendName})" in {
      Driver(() => new SubBytes, backendName) {
        c => new SubBytesUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in the tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new SubBytes) {
      c => new SubBytesUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from the test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new SubBytes) {
      c => new SubBytesUnitTester(c)
    } should be(true)
  }
}
