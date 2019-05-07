/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.lucianms.nio;

public class MaplePacketManipulator {

    private static byte ROL1(byte in, int count) {
        int tmp = (int) in & 0xFF;
        tmp = tmp << (count % 8);
        return (byte) ((tmp & 0xFF) | (tmp >> 8));
    }

    private static byte ROR1(byte in, int count) {
        int tmp = (int) in & 0xFF;
        tmp = (tmp << 8) >>> (count % 8);
        return (byte) ((tmp & 0xFF) | (tmp >>> 8));
    }

    public static void encryptData(byte[] data) {
        for (int j = 0; j < 6; j++) {
            byte remember = 0;
            byte dataLength = (byte) (data.length & 0xFF);
            if (j % 2 == 0) {
                for (int i = 0; i < data.length; i++) {
                    byte cur = data[i];
                    cur = ROL1(cur, 3);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur = ROR1(cur, (int) dataLength & 0xFF);
                    cur = ((byte) ((~cur) & 0xFF));
                    cur += 72;
                    dataLength--;
                    data[i] = cur;
                }
            } else {
                for (int i = data.length - 1; i >= 0; i--) {
                    byte cur = data[i];
                    cur = ROL1(cur, 4);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur ^= 19;
                    cur = ROR1(cur, 3);
                    dataLength--;
                    data[i] = cur;
                }
            }
        }
    }

    public static void decryptData(byte[] data) {
        for (int j = 1; j <= 6; j++) {
            byte remember = 0;
            byte dataLength = (byte) (data.length & 0xFF);
            byte nextRemember;
            if (j % 2 == 0) {
                for (int i = 0; i < data.length; i++) {
                    byte cur = data[i];
                    cur -= 72;
                    cur = ((byte) ((~cur) & 0xFF));
                    cur = ROL1(cur, (int) dataLength & 0xFF);
                    nextRemember = cur;
                    cur ^= remember;
                    remember = nextRemember;
                    cur -= dataLength;
                    cur = ROR1(cur, 3);
                    data[i] = cur;
                    dataLength--;
                }
            } else {
                for (int i = data.length - 1; i >= 0; i--) {
                    byte cur = data[i];
                    cur = ROL1(cur, 3);
                    cur ^= 19;
                    nextRemember = cur;
                    cur ^= remember;
                    remember = nextRemember;
                    cur -= dataLength;
                    cur = ROR1(cur, 4);
                    data[i] = cur;
                    dataLength--;
                }
            }
        }
    }
}
