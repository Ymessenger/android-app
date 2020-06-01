/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package y.encrypt;

public class YEncrypt
{
    byte[] password;
    long masterPaswordId;

    byte[] symmetricEncryptKey;

    byte[] publicEncryptKeyToSend;
    byte[] privateSignKeyToSend;

    byte[] privateEncryptKeyToReceive;
    byte[] publicSignKeyToReceive;

    public YEncrypt(byte[] password, long masterPaswordId)
    {
        this.password = password;
        this.masterPaswordId=masterPaswordId;
    }

    public int getLenPublicEncryptKeyToSend()
    {
        return publicEncryptKeyToSend.length;
    }

    public void getPublicEncryptKeyToSend(byte[] buffer)
    {
        for(int i=0; i<publicEncryptKeyToSend.length; i++)
        {
            buffer[i] = publicEncryptKeyToSend[i];
        }
    }

    public int getLenPrivateSignKeyToSend()
    {
        return privateSignKeyToSend.length;
    }

    public void getPrivateSignKeyToSend(byte[] buffer)
    {
        for(int i=0; i<privateSignKeyToSend.length; i++)
        {
            buffer[i] = privateSignKeyToSend[i];
        }
    }

    public int getLenPrivateEncryptKeyToReceive()
    {
        return privateEncryptKeyToReceive.length;
    }

    public void getPrivateEncryptKeyToReceive(byte[] buffer)
    {
        for(int i=0; i<privateEncryptKeyToReceive.length; i++)
        {
            buffer[i] = privateEncryptKeyToReceive[i];
        }
    }

    public int getLenPublicSignKeyToReceive()
    {
        return publicSignKeyToReceive.length;
    }

    public void getPublicSignKeyToReceive(byte[] buffer)
    {
        for(int i=0; i<publicSignKeyToReceive.length; i++)
        {
            buffer[i] = publicSignKeyToReceive[i];
        }
    }

    public long getMasterPaswordId()
    {
        return masterPaswordId;
    }

    public int getLenPassword()
    {
        return password.length;
    }

    public void getPassword(byte[] buffer)
    {
        for(int i=0; i<password.length; i++)
        {
            buffer[i] = password[i];
        }
    }

    public int getLenSymmetricEncryptKey()
    {
        return symmetricEncryptKey.length;
    }

    private void getSymmetricEncryptKey(byte[] buffer)
    {
        for(int i=0; i<symmetricEncryptKey.length; i++)
        {
            buffer[i] = symmetricEncryptKey[i];
        }
    }

    public byte[] getPublicEncryptKeyToSend() {
        return publicEncryptKeyToSend;
    }

    public byte[] getPrivateSignKeyToSend() {
        return privateSignKeyToSend;
    }

    public byte[] getPrivateEncryptKeyToReceive() {
        return privateEncryptKeyToReceive;
    }

    public byte[] getPublicSignKeyToReceive() {
        return publicSignKeyToReceive;
    }

    public static native void init(byte[] seed);

    public static native void addRandom(byte[] randomByte);

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public void setSymmetricEncryptKey(byte[] symmetricEncryptKey) {
        this.symmetricEncryptKey = symmetricEncryptKey;
    }

    public void setPublicEncryptKeyToSend(byte[] publicEncryptKeyToSend) {
        this.publicEncryptKeyToSend = publicEncryptKeyToSend;
    }

    public void setPrivateSignKeyToSend(byte[] privateSignKeyToSend) {
        this.privateSignKeyToSend = privateSignKeyToSend;
    }

    public void setPrivateEncryptKeyToReceive(byte[] privateEncryptKeyToReceive) {
        this.privateEncryptKeyToReceive = privateEncryptKeyToReceive;
    }

    public void setPublicSignKeyToReceive(byte[] publicSignKeyToReceive) {
        this.publicSignKeyToReceive = publicSignKeyToReceive;
    }


    public native byte[] getSymmetricKey(long version, long id, long timeLife);

    public native KeyPair getShortAsymmetricKeys(long  version, long id, long timeLife, boolean isSign);
    public native KeyPair getMidleAsymmetricKeys(long version, long id, long timeLife, boolean isSign);
    public native KeyPair getLongAsymmetricKeys(long version, long id, long timeLife, boolean isSign);

    public native byte[] encryptSecretMsg(long version, long type, long timeLife, byte[] secretMsg);
    public native byte[] signMsg(long version, long type, long timeLife, byte[] secretMsg);
    public native byte[] encrypKeysMsg(long version, long timeLife, byte[] keys);

    public native DecrypteMsg decryptSecretMsg(byte[] secretMsg);
    public native DecrypteMsg veryfiMsg(byte [] signMsg);
    public native Key decrypKeysMsg(byte [] keyMsg);

    public native byte[] getHashKey(long version, byte [] key_);

    public native MetaData getMetaData(byte[] msg);

    public static native byte[] GenerateKey(long version, byte[] password, byte[] salt, long id);
    public static native byte[] EncryptData(long version, byte[] data, byte[] key);
    public static native byte[] DecryptData(long version, byte[] data, byte[] key);

    static {
        System.loadLibrary("native-lib");
    }
}
