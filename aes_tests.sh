#!/usr/bin/env bash
# script to run all tests at once

echo -e "SubBytesTester:\n"
sbt 'testOnly aes.SubBytesTester'

echo -e "ShiftRowsTester:\n"
sbt 'testOnly aes.ShiftRowsTester'

echo -e "MixColumnsTester:\n"
sbt 'testOnly aes.MixColumnsTester'

echo -e "InvSubBytesTester:\n"
sbt 'testOnly aes.InvSubBytesTester'

echo -e "InvShiftRowsTester:\n"
sbt 'testOnly aes.InvShiftRowsTester'

echo -e "InvMixColumnsTester:\n"
sbt 'testOnly aes.InvMixColumnsTester'

echo -e "AddRoundKeyTester:\n"
sbt 'testOnly aes.AddRoundKeyTester'

echo -e "CipherTester:\n"
sbt 'testOnly aes.CipherTester'

echo -e "InvCipherTester:\n"
sbt 'testOnly aes.InvCipherTester'

echo -e "AESTester:\n"
sbt 'testOnly aes.AESTester'


echo "Tests Finished!"