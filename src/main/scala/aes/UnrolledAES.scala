package aes

import chisel3._
import chisel3.util._

// implements wrapper for unrolled AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
// change expandedKeyMemType= ROM, Mem, SyncReadMem
// change unrolled=[1..Nrplus1] for unroll depth
class UnrolledAES(Nk: Int, unrolled: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  require(expandedKeyMemType == "ROM" || expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem")
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1
  val EKDepth: Int = 16 // enough memory for any expanded key
  require((unrolled > 0) && (unrolled < Nrplus1))

  val io = IO(new Bundle {
    val AES_mode = Input(UInt(2.W)) //  0=00=off, 1=01=expanded key update, 2=10=cipher, 3=11=inverse cipher
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext, roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Declare instances and array of Cipher and Inverse Cipher Rounds
  val CipherRoundARK = CipherRound("AddRoundKeyOnly", SubBytes_SCD)
  val CipherRounds = Array.fill(Nr - 1) {
    Module(new CipherRound("CompleteRound", SubBytes_SCD)).io
  }
  val CipherRoundNMC = CipherRound("NoMixColumns", SubBytes_SCD)

  val InvCipherRoundARK = InvCipherRound("AddRoundKeyOnly", InvSubBytes_SCD)
  val InvCipherRounds = Array.fill(Nr - 1) {
    Module(new InvCipherRound("CompleteRound", InvSubBytes_SCD)).io
  }
  val InvCipherRoundNMC = InvCipherRound("NoInvMixColumns", InvSubBytes_SCD)

  // A roundKey is Params.StateLength bytes, and 1+(10/12/14) (< EKDepth) of them are needed
  // Mem = combinational/asynchronous-read, sequential/synchronous-write = register banks
  // Create a asynchronous-read, synchronous-write memory block big enough for any key length
  //  val expandedKeyARMem = Mem(EKDepth, Vec(Params.StateLength, UInt(8.W)))

  // SyncReadMem = sequential/synchronous-read, sequential/synchronous-write = SRAMs
  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  //  val expandedKeySRMem = SyncReadMem(EKDepth, Vec(Params.StateLength, UInt(8.W)))

  // use the same address and dataOut val elements to interface with the parameterized memory
  val address = RegInit(0.U(log2Ceil(EKDepth).W))
  //  val dataOut = Wire(Vec(Params.StateLength, UInt(8.W)))

  //
  val expandedKeys = Reg(Vec(16, Vec(Params.StateLength, UInt(8.W))))


  when(io.AES_mode === 1.U) { // write to memory
    //    if (expandedKeyMemType == "Mem") {
    //      expandedKeyARMem.write(address, io.input_text)
    //    }
    //    else if (expandedKeyMemType == "SyncReadMem") {
    //      expandedKeySRMem.write(address, io.input_text)
    //    }
    //    dataOut := DontCare
    address := address + 1.U
    expandedKeys(address) := io.input_text
  }
  //    .otherwise { // read from memory
  //      if (expandedKeyMemType == "Mem") {
  //        dataOut := expandedKeyARMem.read(address)
  //      }
  //      else if (expandedKeyMemType == "SyncReadMem") {
  //        dataOut := expandedKeySRMem.read(address)
  //      }
  //      else if (expandedKeyMemType == "ROM") {
  //        Nk match {
  //          case 4 => dataOut := ROMeKeys.expandedKey128(address)
  //          case 6 => dataOut := ROMeKeys.expandedKey192(address)
  //          case 8 => dataOut := ROMeKeys.expandedKey256(address)
  //        }
  //      }
  //
  //      // address logistics
  //      when(
  //        if ((expandedKeyMemType == "Mem") || (expandedKeyMemType == "ROM")) {
  //          (ShiftRegister(io.AES_mode, 1) === 2.U) // delay by 1 for Mem and ROM
  //        }
  //        else {
  //          (io.AES_mode === 2.U) // no delay for SyncReadMem
  //        }
  //      ) {
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

  //// Wire Cipher modules together

  // Cipher ARK round
  when(io.AES_mode === 2.U) { // cipher mode
    CipherRoundARK.io.input_valid := true.B
    CipherRoundARK.io.state_in := io.input_text
    CipherRoundARK.io.roundKey := expandedKeys(0)
  }.otherwise {
    CipherRoundARK.io.input_valid := false.B
    CipherRoundARK.io.state_in := DontCare
    CipherRoundARK.io.roundKey := DontCare
  }

  // Cipher Nr-1 rounds
  for (i <- 0 until (Nr - 1)) yield {
    if (i == 0) {
      CipherRounds(i).input_valid := CipherRoundARK.io.output_valid
      CipherRounds(i).state_in := CipherRoundARK.io.state_out
    }
    else {
      CipherRounds(i).input_valid := CipherRounds(i - 1).output_valid
      CipherRounds(i).state_in := CipherRounds(i - 1).state_out
    }
    CipherRounds(i).roundKey := expandedKeys(i + 1)
  }

  // Cipher last round
  CipherRoundNMC.io.input_valid := CipherRounds(Nr - 1 - 1).output_valid
  CipherRoundNMC.io.state_in := CipherRounds(Nr - 1 - 1).state_out
  CipherRoundNMC.io.roundKey := expandedKeys(Nr)

  //// Wire Inverse Cipher modules together

  // InvCipher ARK round
  when(io.AES_mode === 3.U) { // cipher mode
    InvCipherRoundARK.io.input_valid := true.B
    InvCipherRoundARK.io.state_in := io.input_text
    InvCipherRoundARK.io.roundKey := expandedKeys(Nr)
  }.otherwise {
    InvCipherRoundARK.io.input_valid := false.B
    InvCipherRoundARK.io.state_in := DontCare
    InvCipherRoundARK.io.roundKey := DontCare
  }

  // Cipher Nr-1 rounds
  for (i <- 0 until (Nr - 1)) yield {
    if (i == 0) {
      InvCipherRounds(i).input_valid := InvCipherRoundARK.io.output_valid
      InvCipherRounds(i).state_in := InvCipherRoundARK.io.state_out
    }
    else {
      InvCipherRounds(i).input_valid := InvCipherRounds(i - 1).output_valid
      InvCipherRounds(i).state_in := InvCipherRounds(i - 1).state_out
    }
    InvCipherRounds(i).roundKey := expandedKeys(Nr - i - 1)
  }

  // Cipher last round
  InvCipherRoundNMC.io.input_valid := InvCipherRounds(Nr - 1 - 1).output_valid
  InvCipherRoundNMC.io.state_in := InvCipherRounds(Nr - 1 - 1).state_out
  InvCipherRoundNMC.io.roundKey := expandedKeys(0)

  //  io.output_text := CipherRoundNMC.io.state_out //Mux((io.AES_mode === 2.U), InvCipherRoundNMC.io.state_out, CipherRoundNMC.io.state_out)
  io.output_text := Mux(CipherRoundNMC.io.output_valid, CipherRoundNMC.io.state_out, InvCipherRoundNMC.io.state_out)
  io.output_valid := CipherRoundNMC.io.output_valid || InvCipherRoundNMC.io.output_valid

}

object UnrolledAES {
  def apply(Nk: Int, unrolled: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String): UnrolledAES = Module(new UnrolledAES(Nk, unrolled, SubBytes_SCD, InvSubBytes_SCD, expandedKeyMemType))
}

//// AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
//io.output_valid := io.start
//// AES output can be managed using a Mux on the Cipher output and the InvCipher output
////io.output_text := Mux(io.AES_mode(1), InvCipherRoundModuleNMC.io.state_out, CipherRoundModuleNMC.io.state_out)
//
//io.output_text := expandedKey256_00
//// Debug statements
////printf("address=%d, rom_dataOut=%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x \n", address, ROMeKeyOut(0), ROMeKeyOut(1), ROMeKeyOut(2), ROMeKeyOut(3), ROMeKeyOut(4), ROMeKeyOut(5), ROMeKeyOut(6), ROMeKeyOut(7), ROMeKeyOut(8), ROMeKeyOut(9), ROMeKeyOut(10), ROMeKeyOut(11), ROMeKeyOut(12), ROMeKeyOut(13), ROMeKeyOut(14), ROMeKeyOut(15))
////printf("address=%d, mem_dataOut=%x \n", address, dataOut)
////printf("AES mode=%b start=%b, mem_address=%d, mem_dataOut=%x \n", io.AES_mode, io.start, address, dataOut)
//
////val expandedKey256 = VecInit(Array(
//val expandedKey256_00 = VecInit(Array(0x00.U, 0x01.U, 0x02.U, 0x03.U, 0x04.U, 0x05.U, 0x06.U, 0x07.U, 0x08.U, 0x09.U, 0x0a.U, 0x0b.U, 0x0c.U, 0x0d.U, 0x0e.U, 0x0f.U))
//val expandedKey256_01 = VecInit(Array(0x10.U, 0x11.U, 0x12.U, 0x13.U, 0x14.U, 0x15.U, 0x16.U, 0x17.U, 0x18.U, 0x19.U, 0x1a.U, 0x1b.U, 0x1c.U, 0x1d.U, 0x1e.U, 0x1f.U))
//val expandedKey256_02 = VecInit(Array(0xa5.U, 0x73.U, 0xc2.U, 0x9f.U, 0xa1.U, 0x76.U, 0xc4.U, 0x98.U, 0xa9.U, 0x7f.U, 0xce.U, 0x93.U, 0xa5.U, 0x72.U, 0xc0.U, 0x9c.U))
//val expandedKey256_03 = VecInit(Array(0x16.U, 0x51.U, 0xa8.U, 0xcd.U, 0x02.U, 0x44.U, 0xbe.U, 0xda.U, 0x1a.U, 0x5d.U, 0xa4.U, 0xc1.U, 0x06.U, 0x40.U, 0xba.U, 0xde.U))
//val expandedKey256_04 = VecInit(Array(0xae.U, 0x87.U, 0xdf.U, 0xf0.U, 0x0f.U, 0xf1.U, 0x1b.U, 0x68.U, 0xa6.U, 0x8e.U, 0xd5.U, 0xfb.U, 0x03.U, 0xfc.U, 0x15.U, 0x67.U))
//val expandedKey256_05 = VecInit(Array(0x6d.U, 0xe1.U, 0xf1.U, 0x48.U, 0x6f.U, 0xa5.U, 0x4f.U, 0x92.U, 0x75.U, 0xf8.U, 0xeb.U, 0x53.U, 0x73.U, 0xb8.U, 0x51.U, 0x8d.U))
//val expandedKey256_06 = VecInit(Array(0xc6.U, 0x56.U, 0x82.U, 0x7f.U, 0xc9.U, 0xa7.U, 0x99.U, 0x17.U, 0x6f.U, 0x29.U, 0x4c.U, 0xec.U, 0x6c.U, 0xd5.U, 0x59.U, 0x8b.U))
//val expandedKey256_07 = VecInit(Array(0x3d.U, 0xe2.U, 0x3a.U, 0x75.U, 0x52.U, 0x47.U, 0x75.U, 0xe7.U, 0x27.U, 0xbf.U, 0x9e.U, 0xb4.U, 0x54.U, 0x07.U, 0xcf.U, 0x39.U))
//val expandedKey256_08 = VecInit(Array(0x0b.U, 0xdc.U, 0x90.U, 0x5f.U, 0xc2.U, 0x7b.U, 0x09.U, 0x48.U, 0xad.U, 0x52.U, 0x45.U, 0xa4.U, 0xc1.U, 0x87.U, 0x1c.U, 0x2f.U))
//val expandedKey256_09 = VecInit(Array(0x45.U, 0xf5.U, 0xa6.U, 0x60.U, 0x17.U, 0xb2.U, 0xd3.U, 0x87.U, 0x30.U, 0x0d.U, 0x4d.U, 0x33.U, 0x64.U, 0x0a.U, 0x82.U, 0x0a.U))
//val expandedKey256_10 = VecInit(Array(0x7c.U, 0xcf.U, 0xf7.U, 0x1c.U, 0xbe.U, 0xb4.U, 0xfe.U, 0x54.U, 0x13.U, 0xe6.U, 0xbb.U, 0xf0.U, 0xd2.U, 0x61.U, 0xa7.U, 0xdf.U))
//val expandedKey256_11 = VecInit(Array(0xf0.U, 0x1a.U, 0xfa.U, 0xfe.U, 0xe7.U, 0xa8.U, 0x29.U, 0x79.U, 0xd7.U, 0xa5.U, 0x64.U, 0x4a.U, 0xb3.U, 0xaf.U, 0xe6.U, 0x40.U))
//val expandedKey256_12 = VecInit(Array(0x25.U, 0x41.U, 0xfe.U, 0x71.U, 0x9b.U, 0xf5.U, 0x00.U, 0x25.U, 0x88.U, 0x13.U, 0xbb.U, 0xd5.U, 0x5a.U, 0x72.U, 0x1c.U, 0x0a.U))
//val expandedKey256_13 = VecInit(Array(0x4e.U, 0x5a.U, 0x66.U, 0x99.U, 0xa9.U, 0xf2.U, 0x4f.U, 0xe0.U, 0x7e.U, 0x57.U, 0x2b.U, 0xaa.U, 0xcd.U, 0xf8.U, 0xcd.U, 0xea.U))
//val expandedKey256_14 = VecInit(Array(0x24.U, 0xfc.U, 0x79.U, 0xcc.U, 0xbf.U, 0x09.U, 0x79.U, 0xe9.U, 0x37.U, 0x1a.U, 0xc2.U, 0x3c.U, 0x6d.U, 0x68.U, 0xde.U, 0x36.U))
//
//
//private val transform = "CompleteRound" // AddRoundKeyOnly NoMixColumns CompleteRound
////  private val SRM_args = Seq()
////  val dataOut = Wire(Vec(Params.StateLength, UInt(8.W)))
//val CRMs = for (i <- 0 until Nrplus1) yield {
//  val CRM = Module(new CipherRound(transform, SubBytes_SCD))
//  //    if (i == 0) {
//  //      CRM.io.state_in := expandedKey256_07
//  //      CRM.io.roundKey := expandedKey256_14
//  //    } else if (i < Nrplus1) {
//  CRM.io.state_in := expandedKey256_07
//  CRM.io.roundKey := expandedKey256_14
//  //    }
//}
////  val CRMsIO = Vec.fill(CRMs.map(_.io))

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