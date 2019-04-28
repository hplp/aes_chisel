package aes

import chisel3._
import chisel3.util.{Cat, log2Ceil}

// implements wrapper for AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
// change expandedKeyMemType= ROM, Mem, SyncReadMem
class UnrolledAES(Nk: Int, unrolled: Boolean, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  require(expandedKeyMemType == "ROM" || expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem")
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1
  val EKDepth: Int = 16 // enough memory for any expanded key

  val io = IO(new Bundle {
    val AES_mode = Input(UInt(2.W)) //  0=00=off, 1=01=expanded key update, 2=10=cipher, 3=11=inverse cipher
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext, roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })


  // ROM = configuration that stores the expanded key in ROM
  val initValues = Seq.fill(Params.StateLength) {
    0.U(8.W)
  }
  val ROMeKeyOut = RegInit(VecInit(initValues))

  //val expandedKey256 = VecInit(Array(
  val expandedKey256_00 = VecInit(Array(0x00.U, 0x01.U, 0x02.U, 0x03.U, 0x04.U, 0x05.U, 0x06.U, 0x07.U, 0x08.U, 0x09.U, 0x0a.U, 0x0b.U, 0x0c.U, 0x0d.U, 0x0e.U, 0x0f.U))
  val expandedKey256_01 = VecInit(Array(0x10.U, 0x11.U, 0x12.U, 0x13.U, 0x14.U, 0x15.U, 0x16.U, 0x17.U, 0x18.U, 0x19.U, 0x1a.U, 0x1b.U, 0x1c.U, 0x1d.U, 0x1e.U, 0x1f.U))
  val expandedKey256_02 = VecInit(Array(0xa5.U, 0x73.U, 0xc2.U, 0x9f.U, 0xa1.U, 0x76.U, 0xc4.U, 0x98.U, 0xa9.U, 0x7f.U, 0xce.U, 0x93.U, 0xa5.U, 0x72.U, 0xc0.U, 0x9c.U))
  val expandedKey256_03 = VecInit(Array(0x16.U, 0x51.U, 0xa8.U, 0xcd.U, 0x02.U, 0x44.U, 0xbe.U, 0xda.U, 0x1a.U, 0x5d.U, 0xa4.U, 0xc1.U, 0x06.U, 0x40.U, 0xba.U, 0xde.U))
  val expandedKey256_04 = VecInit(Array(0xae.U, 0x87.U, 0xdf.U, 0xf0.U, 0x0f.U, 0xf1.U, 0x1b.U, 0x68.U, 0xa6.U, 0x8e.U, 0xd5.U, 0xfb.U, 0x03.U, 0xfc.U, 0x15.U, 0x67.U))
  val expandedKey256_05 = VecInit(Array(0x6d.U, 0xe1.U, 0xf1.U, 0x48.U, 0x6f.U, 0xa5.U, 0x4f.U, 0x92.U, 0x75.U, 0xf8.U, 0xeb.U, 0x53.U, 0x73.U, 0xb8.U, 0x51.U, 0x8d.U))
  val expandedKey256_06 = VecInit(Array(0xc6.U, 0x56.U, 0x82.U, 0x7f.U, 0xc9.U, 0xa7.U, 0x99.U, 0x17.U, 0x6f.U, 0x29.U, 0x4c.U, 0xec.U, 0x6c.U, 0xd5.U, 0x59.U, 0x8b.U))
  val expandedKey256_07 = VecInit(Array(0x3d.U, 0xe2.U, 0x3a.U, 0x75.U, 0x52.U, 0x47.U, 0x75.U, 0xe7.U, 0x27.U, 0xbf.U, 0x9e.U, 0xb4.U, 0x54.U, 0x07.U, 0xcf.U, 0x39.U))
  val expandedKey256_08 = VecInit(Array(0x0b.U, 0xdc.U, 0x90.U, 0x5f.U, 0xc2.U, 0x7b.U, 0x09.U, 0x48.U, 0xad.U, 0x52.U, 0x45.U, 0xa4.U, 0xc1.U, 0x87.U, 0x1c.U, 0x2f.U))
  val expandedKey256_09 = VecInit(Array(0x45.U, 0xf5.U, 0xa6.U, 0x60.U, 0x17.U, 0xb2.U, 0xd3.U, 0x87.U, 0x30.U, 0x0d.U, 0x4d.U, 0x33.U, 0x64.U, 0x0a.U, 0x82.U, 0x0a.U))
  val expandedKey256_10 = VecInit(Array(0x7c.U, 0xcf.U, 0xf7.U, 0x1c.U, 0xbe.U, 0xb4.U, 0xfe.U, 0x54.U, 0x13.U, 0xe6.U, 0xbb.U, 0xf0.U, 0xd2.U, 0x61.U, 0xa7.U, 0xdf.U))
  val expandedKey256_11 = VecInit(Array(0xf0.U, 0x1a.U, 0xfa.U, 0xfe.U, 0xe7.U, 0xa8.U, 0x29.U, 0x79.U, 0xd7.U, 0xa5.U, 0x64.U, 0x4a.U, 0xb3.U, 0xaf.U, 0xe6.U, 0x40.U))
  val expandedKey256_12 = VecInit(Array(0x25.U, 0x41.U, 0xfe.U, 0x71.U, 0x9b.U, 0xf5.U, 0x00.U, 0x25.U, 0x88.U, 0x13.U, 0xbb.U, 0xd5.U, 0x5a.U, 0x72.U, 0x1c.U, 0x0a.U))
  val expandedKey256_13 = VecInit(Array(0x4e.U, 0x5a.U, 0x66.U, 0x99.U, 0xa9.U, 0xf2.U, 0x4f.U, 0xe0.U, 0x7e.U, 0x57.U, 0x2b.U, 0xaa.U, 0xcd.U, 0xf8.U, 0xcd.U, 0xea.U))
  val expandedKey256_14 = VecInit(Array(0x24.U, 0xfc.U, 0x79.U, 0xcc.U, 0xbf.U, 0x09.U, 0x79.U, 0xe9.U, 0x37.U, 0x1a.U, 0xc2.U, 0x3c.U, 0x6d.U, 0x68.U, 0xde.U, 0x36.U))

  //TODO: update this Mem implementation to make use of Masks

  //  // A roundKey is Params.StateLength bytes, and 1+(10/12/14) (< EKDepth) of them are needed
  //  // Mem = combinational/asynchronous-read, sequential/synchronous-write = register banks
  //  // Create a asynchronous-read, synchronous-write memory block big enough for any key length
  //  val expandedKeyARMem = Mem(EKDepth, UInt((Params.StateLength * 8).W))
  //
  //  // SyncReadMem = sequential/synchronous-read, sequential/synchronous-write = SRAMs
  //  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  //  val expandedKeySRMem = SyncReadMem(EKDepth, UInt((Params.StateLength * 8).W))
  //
  //  // use the same address and dataOut val elements to interface with the parameterized memory
  //  val address = RegInit(0.U(log2Ceil(EKDepth).W))
  //  val dataOut = RegInit(0.U((Params.StateLength * 8).W))

  //  when(io.AES_mode === 1.U) { // write to memory
  //    if (expandedKeyMemType == "Mem") {
  //      expandedKeyARMem(address) := Cat(io.input_text(0), io.input_text(1), io.input_text(2), io.input_text(3), io.input_text(4), io.input_text(5), io.input_text(6), io.input_text(7), io.input_text(8), io.input_text(9), io.input_text(10), io.input_text(11), io.input_text(12), io.input_text(13), io.input_text(14), io.input_text(15))
  //    }
  //    else if (expandedKeyMemType == "SyncReadMem") {
  //      expandedKeySRMem(address) := Cat(io.input_text(0), io.input_text(1), io.input_text(2), io.input_text(3), io.input_text(4), io.input_text(5), io.input_text(6), io.input_text(7), io.input_text(8), io.input_text(9), io.input_text(10), io.input_text(11), io.input_text(12), io.input_text(13), io.input_text(14), io.input_text(15))
  //    }
  //    dataOut := DontCare
  //    address := address + 1.U
  //  }
  //    .otherwise { // read from memory
  //      if (expandedKeyMemType == "Mem") {
  //        dataOut := expandedKeyARMem(address)
  //      }
  //      else if (expandedKeyMemType == "SyncReadMem") {
  //        dataOut := expandedKeySRMem(address)
  //      }
  //
  //      // address logistics
  //      when(io.AES_mode === 2.U) {
  //        address := address + 1.U
  //      }
  //        .elsewhen(io.AES_mode === 3.U) {
  //          when(address === 0.U) {
  //            address := Nr.U
  //          }
  //            .otherwise {
  //              address := address - 1.U
  //            }
  //        }
  //        .otherwise {
  //          address := 0.U
  //        }
  //    }

  //  if (expandedKeyMemType == "ROM") {
  //    for (i <- 0 to Params.StateLength - 1) {
  //      if (Nk == 4) {
  //        ROMeKeyOut(i.U) := ROMeKeys.expandedKey128(address * Params.StateLength.U + i.U)
  //      } else if (Nk == 6) {
  //        ROMeKeyOut(i.U) := ROMeKeys.expandedKey192(address * Params.StateLength.U + i.U)
  //      } else if (Nk == 8) {
  //        ROMeKeyOut(i.U) := ROMeKeys.expandedKey256(address * Params.StateLength.U + i.U)
  //      }
  //    }
  //  }


  // Instantiate CipherRound module objects
  val CipherRoundModuleARK = CipherRound("AddRoundKeyOnly", SubBytes_SCD)
  val CipherRoundModuleCNr = Vec(Seq.fill(Nr/2-1) {Module(new CipherRound("CompleteRound", SubBytes_SCD)).io})
  val CipherRoundModuleNMC = CipherRound("NoMixColumns", SubBytes_SCD)

  CipherRoundModuleARK.io.state_in := io.input_text
  CipherRoundModuleARK.io.roundKey := expandedKey256_00

  CipherRoundModuleCNr(0).state_in := CipherRoundModuleARK.io.state_out
  CipherRoundModuleCNr(0).roundKey := expandedKey256_01

  CipherRoundModuleCNr(1).state_in := CipherRoundModuleCNr(0).state_out
  CipherRoundModuleCNr(1).roundKey := expandedKey256_02

  CipherRoundModuleCNr(2).state_in := CipherRoundModuleCNr(1).state_out
  CipherRoundModuleCNr(2).roundKey := expandedKey256_03

  CipherRoundModuleCNr(3).state_in := CipherRoundModuleCNr(2).state_out
  CipherRoundModuleCNr(3).roundKey := expandedKey256_04

  CipherRoundModuleCNr(4).state_in := CipherRoundModuleCNr(3).state_out
  CipherRoundModuleCNr(4).roundKey := expandedKey256_05

  CipherRoundModuleCNr(5).state_in := CipherRoundModuleCNr(4).state_out
  CipherRoundModuleCNr(5).roundKey := expandedKey256_06

//  CipherRoundModuleCNr(6).state_in := CipherRoundModuleCNr(5).state_out
//  CipherRoundModuleCNr(6).roundKey := expandedKey256_07
//
//  CipherRoundModuleCNr(7).state_in := CipherRoundModuleCNr(6).state_out
//  CipherRoundModuleCNr(7).roundKey := expandedKey256_08
//
//  CipherRoundModuleCNr(8).state_in := CipherRoundModuleCNr(7).state_out
//  CipherRoundModuleCNr(8).roundKey := expandedKey256_09
//
//  CipherRoundModuleCNr(9).state_in := CipherRoundModuleCNr(8).state_out
//  CipherRoundModuleCNr(9).roundKey := expandedKey256_10
//
//  CipherRoundModuleCNr(10).state_in := CipherRoundModuleCNr(9).state_out
//  CipherRoundModuleCNr(10).roundKey := expandedKey256_11
//
//  CipherRoundModuleCNr(11).state_in := CipherRoundModuleCNr(10).state_out
//  CipherRoundModuleCNr(11).roundKey := expandedKey256_12
//
//  CipherRoundModuleCNr(12).state_in := CipherRoundModuleCNr(11).state_out
//  CipherRoundModuleCNr(12).roundKey := expandedKey256_13

  CipherRoundModuleNMC.io.state_in := CipherRoundModuleCNr(5).state_out
  CipherRoundModuleNMC.io.roundKey := expandedKey256_14

  // Instantiate CipherRound module objects
  val InvCipherRoundModuleARK = InvCipherRound("AddRoundKeyOnly", SubBytes_SCD)
  val InvCipherRoundModuleCNr = Vec(Seq.fill(Nr/2-1) {Module(new InvCipherRound("CompleteRound", SubBytes_SCD)).io})
  val InvCipherRoundModuleNMC = InvCipherRound("NoMixColumns", SubBytes_SCD)

  InvCipherRoundModuleARK.io.state_in := io.input_text
  InvCipherRoundModuleARK.io.roundKey := expandedKey256_14

  InvCipherRoundModuleCNr(0).state_in := InvCipherRoundModuleARK.io.state_out
  InvCipherRoundModuleCNr(0).roundKey := expandedKey256_13

  InvCipherRoundModuleCNr(1).state_in := InvCipherRoundModuleCNr(0).state_out
  InvCipherRoundModuleCNr(1).roundKey := expandedKey256_12

  InvCipherRoundModuleCNr(2).state_in := InvCipherRoundModuleCNr(1).state_out
  InvCipherRoundModuleCNr(2).roundKey := expandedKey256_11

  InvCipherRoundModuleCNr(3).state_in := InvCipherRoundModuleCNr(2).state_out
  InvCipherRoundModuleCNr(3).roundKey := expandedKey256_10

  InvCipherRoundModuleCNr(4).state_in := InvCipherRoundModuleCNr(3).state_out
  InvCipherRoundModuleCNr(4).roundKey := expandedKey256_09

  InvCipherRoundModuleCNr(5).state_in := InvCipherRoundModuleCNr(4).state_out
  InvCipherRoundModuleCNr(5).roundKey := expandedKey256_08

//  InvCipherRoundModuleCNr(6).state_in := InvCipherRoundModuleCNr(5).state_out
//  InvCipherRoundModuleCNr(6).roundKey := expandedKey256_07
//
//  InvCipherRoundModuleCNr(7).state_in := InvCipherRoundModuleCNr(6).state_out
//  InvCipherRoundModuleCNr(7).roundKey := expandedKey256_06
//
//  InvCipherRoundModuleCNr(8).state_in := InvCipherRoundModuleCNr(7).state_out
//  InvCipherRoundModuleCNr(8).roundKey := expandedKey256_05
//
//  InvCipherRoundModuleCNr(9).state_in := InvCipherRoundModuleCNr(8).state_out
//  InvCipherRoundModuleCNr(9).roundKey := expandedKey256_04
//
//  InvCipherRoundModuleCNr(10).state_in := InvCipherRoundModuleCNr(9).state_out
//  InvCipherRoundModuleCNr(10).roundKey := expandedKey256_03
//
//  InvCipherRoundModuleCNr(11).state_in := InvCipherRoundModuleCNr(10).state_out
//  InvCipherRoundModuleCNr(11).roundKey := expandedKey256_02
//
//  InvCipherRoundModuleCNr(12).state_in := InvCipherRoundModuleCNr(11).state_out
//  InvCipherRoundModuleCNr(12).roundKey := expandedKey256_01

  InvCipherRoundModuleNMC.io.state_in := InvCipherRoundModuleCNr(5).state_out
  InvCipherRoundModuleNMC.io.roundKey := expandedKey256_00

  //  // The roundKey for each round can go to both the cipher and inverse cipher (for now TODO)
  //  if (expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem") {
  //    CipherModule.io.roundKey := Array(dataOut(127, 120), dataOut(119, 112), dataOut(111, 104), dataOut(103, 96), dataOut(95, 88), dataOut(87, 80), dataOut(79, 72), dataOut(71, 64), dataOut(63, 56), dataOut(55, 48), dataOut(47, 40), dataOut(39, 32), dataOut(31, 24), dataOut(23, 16), dataOut(15, 8), dataOut(7, 0))
  //    InvCipherModule.io.roundKey := Array(dataOut(127, 120), dataOut(119, 112), dataOut(111, 104), dataOut(103, 96), dataOut(95, 88), dataOut(87, 80), dataOut(79, 72), dataOut(71, 64), dataOut(63, 56), dataOut(55, 48), dataOut(47, 40), dataOut(39, 32), dataOut(31, 24), dataOut(23, 16), dataOut(15, 8), dataOut(7, 0))
  //  } else if (expandedKeyMemType == "ROM") {
  //    CipherModule.io.roundKey := ROMeKeyOut
  //    InvCipherModule.io.roundKey := ROMeKeyOut
  //  }

  //  // The input text can go to both the cipher and the inverse cipher (for now TODO)
  //  CipherModule.io.plaintext <> io.input_text
  //  InvCipherModule.io.ciphertext <> io.input_text
  //
  //  // Cipher starts at (start=1 and AES_Mode=2)
  //  CipherModule.io.start := io.start && (io.AES_mode === 2.U)
  //  // Inverse Cipher starts at (start=1 and AES_Mode=1)
  //  InvCipherModule.io.start := io.start && (io.AES_mode === 3.U)

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := io.start
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text := Mux(io.AES_mode(1), InvCipherRoundModuleNMC.io.state_out, CipherRoundModuleNMC.io.state_out)

  // Debug statements
  //printf("address=%d, rom_dataOut=%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x \n", address, ROMeKeyOut(0), ROMeKeyOut(1), ROMeKeyOut(2), ROMeKeyOut(3), ROMeKeyOut(4), ROMeKeyOut(5), ROMeKeyOut(6), ROMeKeyOut(7), ROMeKeyOut(8), ROMeKeyOut(9), ROMeKeyOut(10), ROMeKeyOut(11), ROMeKeyOut(12), ROMeKeyOut(13), ROMeKeyOut(14), ROMeKeyOut(15))
  //printf("address=%d, mem_dataOut=%x \n", address, dataOut)
  //printf("AES mode=%b start=%b, mem_address=%d, mem_dataOut=%x \n", io.AES_mode, io.start, address, dataOut)
}


object UnrolledAES {
  def apply(Nk: Int, unrolled: Boolean, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String): UnrolledAES = Module(new UnrolledAES(Nk, unrolled, SubBytes_SCD, InvSubBytes_SCD, expandedKeyMemType))
}