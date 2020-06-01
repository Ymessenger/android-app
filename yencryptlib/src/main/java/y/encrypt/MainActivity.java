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

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    /*static {
        System.loadLibrary("native-lib");
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method

        String password = "1234567890";

        String password2 = "0987654321";

        String seed = "0987654321";

        byte[] openData = new byte[3000];

        for(int i=0; i <3000; i++)
        {
            openData[i] = (byte)((i%100)+32);
        }

        YEncrypt.init(seed.getBytes());

        YEncrypt test1 = new YEncrypt(password.getBytes(), 10);
        YEncrypt test2 = new YEncrypt(password2.getBytes(), 20);

        byte[] sKey1 = test1.getSymmetricKey(1,1,10000);

        KeyPair pKeySign1 = test1.getShortAsymmetricKeys(1,12,10000, true);
        KeyPair pKeyEncrypt2 = test2.getShortAsymmetricKeys(1, 13, 10000, false);

        test1.setPrivateSignKeyToSend(pKeySign1.getPrivateKey());
        test1.setPublicEncryptKeyToSend(pKeyEncrypt2.getPublicKey());

        byte[] encryptedKey = test1.encrypKeysMsg(1, 10000, sKey1);

        test2.setPrivateEncryptKeyToReceive(pKeyEncrypt2.getPrivateKey());
        test2.setPublicSignKeyToReceive(pKeySign1.getPublicKey());

        Key decryptedKey = test2.decrypKeysMsg(encryptedKey);

        byte [] hash1 = test1.getHashKey(1, sKey1);
        byte [] hash2 = test2.getHashKey(1, decryptedKey.getKey());

        int countDoubleHash = 0;
        for(int i =0; i<hash1.length; i++)
        {
            if(hash1[i]==hash2[i])
                countDoubleHash++;
        }

        test1.setSymmetricEncryptKey(sKey1);
        test2.setSymmetricEncryptKey(decryptedKey.getKey());

        byte [] encryptedData = test1.encryptSecretMsg(1,1,10000, openData);
        DecrypteMsg msg = test2.decryptSecretMsg(encryptedData);

        int countDoubleMsg = 0;
        for(int i=0; i<openData.length; i++)
        {
            if(openData[i]==msg.msg[i])
            {
                countDoubleMsg++;
            }
        }

        byte[] signData = test1.signMsg(1,2,20000,openData);
        DecrypteMsg veryfiData = test2.veryfiMsg(signData);
        int countDoublemsgVeryfi =0;
        for(int i=0; i<openData.length; i++)
        {
            if(openData[i]==veryfiData.msg[i])
            {
                countDoublemsgVeryfi++;
            }
        }

        byte[] passworForKey = "1234567890".getBytes();
        byte[] saltForKey = "0123456789ABCDEF".getBytes();

        byte[] key123 = YEncrypt.GenerateKey(1, passworForKey, saltForKey, 12);
        byte[] key321 = YEncrypt.GenerateKey(1, passworForKey, saltForKey, 14);

        byte[] hash123 = test1.getHashKey(1, key123);
        byte[] hash321 = test1.getHashKey(1, key123);

        int countDoubleHashKey =0;
        for(int i=0; i<hash123.length; i++)
        {
            if(hash123[i]==hash321[i])
            {
                countDoubleHashKey++;
            }
        }

        byte[] encryptedRandomData = YEncrypt.EncryptData(1, openData, key123);
        byte[] decryptedRandomData = YEncrypt.DecryptData(1, encryptedRandomData, key123);

        int countDoubleAftrdecrypt = 0;

        for(int i=0; i< openData.length; i++)
        {
            if(decryptedRandomData[i]==openData[i])
            {
                countDoubleAftrdecrypt++;
            }
        }
        countDoubleAftrdecrypt =0;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
