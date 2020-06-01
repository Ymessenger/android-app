#include <jni.h>
#include <string>
#include "CYSecurityFacad.h"
#include "CYException.h"

class ThrowJavaExeption : public YSecurity::IVisitor
{
    JNIEnv* pEnv;
public:
    ThrowJavaExeption(JNIEnv* pEnv)
    {
        this->pEnv = pEnv;
    }
    void visit( YSecurity::CYException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YSecurityException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYInitializationException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YInitializationException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYPasswordException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YPasswordException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYAlogitmExistedException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YAlogitmExistedException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYRandomStatusException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YRandomStatusException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYGenerateKeyException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YGenerateKeyException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYKeyException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YKeyException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYEncryptException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YEncryptException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYDecryptException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YDecryptException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);

    }
    void visit( YSecurity::CYNoKeyException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YNoKeyException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);

    }
    void visit( YSecurity::CYDataException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YDataException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);

    }
    void visit( YSecurity::CYSignException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YSignException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);

    }
    void visit( YSecurity::CYVerifityException const & _exception )
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YVerifityException");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYTimeLife const & _exception)
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YTimelifeKey");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }
    void visit( YSecurity::CYTimeLifeMsg const & _exception)
    {
        jclass clazz = pEnv->FindClass("y/encrypt/YTimelifeMsg");
        if (clazz != NULL) {
            pEnv->ThrowNew(clazz, _exception.what());
        }
        pEnv->DeleteLocalRef(clazz);
    }

};

void throwException(JNIEnv* pEnv, const char * msg)
{
    jclass clazz = pEnv->FindClass("java/lang/Exception");
    if (clazz != NULL) {
        pEnv->ThrowNew(clazz, msg);
    }
    pEnv->DeleteLocalRef(clazz);
}

jbyteArray converCDataMassToJavaArray(JNIEnv *env, YSecurity::CYData<YSecurity::ubyte>& data)
{
    jbyteArray javaData = env->NewByteArray(data.getLenData());
    jbyte * javaDataPtr = env->GetByteArrayElements(javaData, NULL);

    YSecurity::ubyte * dataPtr = data.getData();
    for(int i=0; i<data.getLenData(); i++)
    {
        javaDataPtr[i] = dataPtr[i];
    }
    env->ReleaseByteArrayElements(javaData, javaDataPtr, 0);
    return javaData;
}

YSecurity::CYData<YSecurity::ubyte > getData(JNIEnv *env, jobject instance, const char * methodLen, const char * methodData)
{
    jclass facad = env->GetObjectClass(instance);
    jmethodID getLenDate = env->GetMethodID(facad, methodLen,"()I");
    jmethodID getDate = env->GetMethodID(facad, methodData, "([B)V");

    jbyteArray javaData = env->NewByteArray(env->CallIntMethod(instance,getLenDate));
    env->CallVoidMethod(instance, getDate, javaData);
    jbyte * javaDataPtr = env->GetByteArrayElements(javaData, NULL);

    const jsize length = env->GetArrayLength(javaData);

    YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)javaDataPtr, length);

    env->ReleaseByteArrayElements(javaData, javaDataPtr, 0);

    env->DeleteLocalRef(javaData);
    return data;
}

YSecurity::CYData<YSecurity::char_t> getPassword(JNIEnv *env, jobject instance)
{
    jclass facad = env->GetObjectClass(instance);
    jmethodID getLenPassword = env->GetMethodID(facad, "getLenPassword","()I");
    jmethodID getPassword = env->GetMethodID(facad, "getPassword","([B)V");


    auto length = env->CallIntMethod(instance,getLenPassword);

    jbyteArray password = env->NewByteArray(length);

    env->CallVoidMethod(instance, getPassword, password);

    jbyte * passwordPtr = env->GetByteArrayElements(password, NULL);

    YSecurity::CYData<YSecurity::char_t> pass((char*)passwordPtr, length);

    env->ReleaseByteArrayElements(password, passwordPtr, 0);

    env->DeleteLocalRef(password);

    return pass;
}

jlong getMasterPasswordId(JNIEnv *env, jobject instance)
{
    jclass facad = env->GetObjectClass(instance);
    jmethodID getMasterPasswordId = env->GetMethodID(facad, "getMasterPaswordId","()J");

    auto id = env->CallLongMethod(instance,getMasterPasswordId);

    return id;
}

YSecurity::CYData<YSecurity::ubyte > getSymmetricEncryptKey(JNIEnv *env, jobject instance)
{
    return getData(env, instance, "getLenSymmetricEncryptKey", "getSymmetricEncryptKey");
}


YSecurity::CYData<YSecurity::ubyte > getPublicEncryptKeyToSend(JNIEnv *env, jobject instance)
{
    return getData(env, instance, "getLenPublicEncryptKeyToSend", "getPublicEncryptKeyToSend");
}

YSecurity::CYData<YSecurity::ubyte > getPrivateSignKeyToSend(JNIEnv *env, jobject instance)
{
    return getData(env, instance, "getLenPrivateSignKeyToSend", "getPrivateSignKeyToSend");
}

YSecurity::CYData<YSecurity::ubyte > getPrivateEncryptKeyToReceive(JNIEnv *env, jobject instance)
{
    return getData(env, instance, "getLenPrivateEncryptKeyToReceive", "getPrivateEncryptKeyToReceive");
}

YSecurity::CYData<YSecurity::ubyte > getPublicSignKeyToReceive(JNIEnv *env, jobject instance)
{
    return getData(env, instance, "getLenPublicSignKeyToReceive", "getPublicSignKeyToReceive");
}

extern "C" JNIEXPORT jstring JNICALL
Java_y_encrypt_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_getSymmetricKey(JNIEnv *env, jobject instance, jlong version, jlong id,
                                        jlong timeLife) {

    try
    {
        jbyteArray  symmetricKey;
        auto pass = getPassword(env, instance);
        auto idMasterPsword = getMasterPasswordId(env, instance);
        YSecurity::CYSecurityFacad facad(pass, idMasterPsword);
        YSecurity::CYKey * k = facad.getSymmetricKey(version, id, timeLife);
        if(k!= nullptr)
        {
            auto ptrKey = std::unique_ptr<YSecurity::CYKey>(k);
            YSecurity::CYData<YSecurity::ubyte> * data = k->getBytesKeyData();
            auto ptrData = std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>>(data);
            symmetricKey = converCDataMassToJavaArray(env,*data);
        }
        return symmetricKey;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

jobject getAsymmetricKeys(JNIEnv *env, jobject instance, int type, jlong version,
                               jlong id, jlong timeLife, bool isSign)
{
    jclass classPair = env->FindClass("y/encrypt/KeyPair");

    jmethodID setPublicKey = env->GetMethodID(classPair, "setPublicKey","([B)V");

    jmethodID setPrivateKey = env->GetMethodID(classPair, "setPrivateKey","([B)V");

    try
    {
        auto pass = getPassword(env, instance);
        auto idMasterPsword = getMasterPasswordId(env, instance);
        YSecurity::CYSecurityFacad facad(pass, idMasterPsword);
        YSecurity::CYKeysPair * k = nullptr;

        if (type == 1)
        {
            k = facad.getShortAsymmetricKeys(version, id, timeLife, isSign);
        }
        else if (type == 2)
        {
            k = facad.getMidleAsymmetricKeys(version, id, timeLife, isSign);
        }
        else
        {
            k = facad.getLongAsymmetricKeys(version, id, timeLife, isSign);
        }

        jobject keyPair = nullptr;

        if(k!= nullptr)
        {
            auto publicKey = k->getPublicKey();
            auto privateKey = k->getPrivateKey();

            YSecurity::CYData<YSecurity::ubyte> * dataPublic = publicKey->getBytesKeyData();
            YSecurity::CYData<YSecurity::ubyte> * dataPrivate = privateKey->getBytesKeyData();

            jbyteArray pubKey = converCDataMassToJavaArray(env, *dataPublic);
            jbyteArray prvKey = converCDataMassToJavaArray(env, *dataPrivate);

            delete k;
            delete dataPublic;
            delete dataPrivate;

            keyPair = env->AllocObject(classPair);
            env->CallVoidMethod(keyPair,setPublicKey, pubKey);
            env->CallVoidMethod(keyPair,setPrivateKey, prvKey);
        }
        return keyPair;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }

    return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_getShortAsymmetricKeys(JNIEnv *env, jobject instance, jlong version,
                                               jlong id, jlong timeLife, jboolean isSign) {

    return getAsymmetricKeys(env, instance, 1, version, id, timeLife, isSign);

}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_getMidleAsymmetricKeys(JNIEnv *env, jobject instance, jlong version,
                                               jlong id, jlong timeLife, jboolean isSign) {

    return getAsymmetricKeys(env, instance, 2, version, id, timeLife, isSign);

}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_getLongAsymmetricKeys(JNIEnv *env, jobject instance, jlong version,
                                              jlong id, jlong timeLife, jboolean isSign) {

    return getAsymmetricKeys(env, instance, 3, version, id, timeLife, isSign);

}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_encryptSecretMsg(JNIEnv *env, jobject instance, jlong version, jlong type,
                                         jlong timeLife, jbyteArray secretMsg_) {
    try
    {
        jbyte *secretMsg = env->GetByteArrayElements(secretMsg_, NULL);
        const jsize length = env->GetArrayLength(secretMsg_);
        YSecurity::CYData<YSecurity::ubyte> open((YSecurity::ubyte*)secretMsg, length);
        env->ReleaseByteArrayElements(secretMsg_, secretMsg, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env,instance);
        auto idMasterPsword = getMasterPasswordId(env, instance);
        YSecurity::CYData<YSecurity::ubyte > keySimmmetryc = getSymmetricEncryptKey(env,instance);
        YSecurity::CYData<YSecurity::ubyte > keySign = getPrivateSignKeyToSend(env,instance);

        YSecurity::CYKey keyEnc(keySimmmetryc.getData(), keySimmmetryc.getLenData());
        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPsword);
        facad.setPrivateSignKeyToSend(&keySig);
        facad.setSymmetricEncryptKey(&keyEnc);
        auto data = facad.encryptSecretMsg(version, type, timeLife, open);

        jbyteArray encryptMsg = converCDataMassToJavaArray(env, data);

        return encryptMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_signMsg(JNIEnv *env, jobject instance, jlong version, jlong type,
        jlong timeLife, jbyteArray data_) {

    try
    {
        jbyte *secretMsg = env->GetByteArrayElements(data_, NULL);
        const jsize length = env->GetArrayLength(data_);
        YSecurity::CYData<YSecurity::ubyte> open((YSecurity::ubyte*)secretMsg, length);
        env->ReleaseByteArrayElements(data_, secretMsg, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env,instance);
        auto idMasterPsword = getMasterPasswordId(env, instance);

        YSecurity::CYData<YSecurity::ubyte > keySign = getPrivateSignKeyToSend(env,instance);

        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPsword);
        facad.setPrivateSignKeyToSend(&keySig);
        auto data = facad.signSecretMsg(version, type, timeLife, open);

        jbyteArray signMsg = converCDataMassToJavaArray(env, data);

        return signMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_encrypKeysMsg(JNIEnv *env, jobject instance, jlong version, jlong timeLife,
                                      jbyteArray keys_) {
    try
    {
        jbyte *keys = env->GetByteArrayElements(keys_, NULL);
        const jsize length = env->GetArrayLength(keys_);
        YSecurity::CYKey openKey((YSecurity::ubyte*)keys, length);
        env->ReleaseByteArrayElements(keys_, keys, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env, instance);
        auto idMasterPassword = getMasterPasswordId(env, instance);

        YSecurity::CYData<YSecurity::ubyte > key = getPublicEncryptKeyToSend(env, instance);
        YSecurity::CYData<YSecurity::ubyte > keySign = getPrivateSignKeyToSend(env, instance);
        YSecurity::CYKey keyEnc(key.getData(), key.getLenData());
        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPassword);
        facad.setPrivateSignKeyToSend(&keySig);
        facad.setPublicEncryptKeyToSend(&keyEnc);

        auto data = facad.encrypKeysMsg(version, timeLife, &openKey);
        jbyteArray encryptMsg = converCDataMassToJavaArray(env, data);
        return encryptMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_decryptSecretMsg(JNIEnv *env, jobject instance, jbyteArray secretMsg_) {

    try
    {
        jbyte * secretMsg = env->GetByteArrayElements(secretMsg_, NULL);
        const jsize length = env->GetArrayLength(secretMsg_);
        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)secretMsg, length);
        env->ReleaseByteArrayElements(secretMsg_, secretMsg, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env,instance);
        auto idMasterPassword = getMasterPasswordId(env, instance);

        YSecurity::CYData<YSecurity::ubyte > key = getSymmetricEncryptKey(env, instance);
        YSecurity::CYData<YSecurity::ubyte > keySign = getPublicSignKeyToReceive(env, instance);

        YSecurity::CYKey keyEnc(key.getData(), key.getLenData());
        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPassword);
        facad.setPublicSignKeyToReceive(&keySig);
        facad.setSymmetricEncryptKey(&keyEnc);
        YSecurity::CYSecretMsg * msg = facad.decryptSecretMsg(data);

        jobject decrypteMsg = nullptr;

        if(msg!= nullptr)
        {
            auto type = msg->getType();
            auto open = msg->getData();
            auto meta = msg->getMeta();
            auto ver = msg->getVer();


            jclass classPair = env->FindClass("y/encrypt/DecrypteMsg");

            jmethodID setMsg = env->GetMethodID(classPair, "setMsg","([B)V");

            jmethodID setType = env->GetMethodID(classPair, "setType","(J)V");
            jmethodID setMeta = env->GetMethodID(classPair, "setMeta","(J)V");
            jmethodID setVer = env->GetMethodID(classPair, "setVersion","(J)V");

            decrypteMsg = env->AllocObject(classPair);

            jbyteArray openData = converCDataMassToJavaArray(env, open);

            env->CallVoidMethod(decrypteMsg, setMsg, openData);
            env->CallVoidMethod(decrypteMsg, setType, (long long)type);
            env->CallVoidMethod(decrypteMsg, setMeta, (long long)meta);
            env->CallVoidMethod(decrypteMsg, setVer,  (long long)ver);

            delete msg;
        }

        return decrypteMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_veryfiMsg(JNIEnv *env, jobject instance, jbyteArray signMsg_) {

    try
    {
        jbyte * secretMsg = env->GetByteArrayElements(signMsg_, NULL);
        const jsize length = env->GetArrayLength(signMsg_);
        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)secretMsg, length);
        env->ReleaseByteArrayElements(signMsg_, secretMsg, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env,instance);
        auto idMasterPassword = getMasterPasswordId(env, instance);

        YSecurity::CYData<YSecurity::ubyte > keySign = getPublicSignKeyToReceive(env, instance);

        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPassword);
        facad.setPublicSignKeyToReceive(&keySig);
        YSecurity::CYSecretMsg * msg = facad.veryfiSecretMsg(data);

        jobject decrypteMsg = nullptr;

        if(msg!= nullptr)
        {
            auto type = msg->getType();
            auto open = msg->getData();
            auto meta = msg->getMeta();
            auto ver = msg->getVer();


            jclass classPair = env->FindClass("y/encrypt/DecrypteMsg");

            jmethodID setMsg = env->GetMethodID(classPair, "setMsg","([B)V");

            jmethodID setType = env->GetMethodID(classPair, "setType","(J)V");
            jmethodID setMeta = env->GetMethodID(classPair, "setMeta","(J)V");
            jmethodID setVer = env->GetMethodID(classPair, "setVersion","(J)V");

            decrypteMsg = env->AllocObject(classPair);

            jbyteArray openData = converCDataMassToJavaArray(env, open);

            env->CallVoidMethod(decrypteMsg, setMsg, openData);
            env->CallVoidMethod(decrypteMsg, setType, (long long)type);
            env->CallVoidMethod(decrypteMsg, setMeta, (long long)meta);
            env->CallVoidMethod(decrypteMsg, setVer,  (long long)ver);

            delete msg;
        }

        return decrypteMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_getMetaData(JNIEnv *env, jobject instance, jbyteArray msg_) {

    try
    {
        jbyte *msg = env->GetByteArrayElements(msg_, NULL);
        const jsize length = env->GetArrayLength(msg_);
        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)msg, length);
        env->ReleaseByteArrayElements(msg_, msg, 0);

        jclass classMeta = env->FindClass("y/encrypt/MetaData");

        jmethodID setVersion = env->GetMethodID(classMeta, "setVersion","(J)V");

        jmethodID setMeta = env->GetMethodID(classMeta, "setMeta","(J)V");

        jmethodID setType = env->GetMethodID(classMeta, "setType","(J)V");

        jmethodID setIdEncryptKey = env->GetMethodID(classMeta, "setIdEncryptKey","(J)V");

        jmethodID setIdSignKey = env->GetMethodID(classMeta, "setIdSignKey","(J)V");

        auto metaData = YSecurity::CYSecurityFacad::getMetaData(data);

        auto uPtrMD = std::unique_ptr<YSecurity::CYMetaMsg>(metaData);

        auto meta = env->AllocObject(classMeta);

        env->CallVoidMethod(meta, setVersion, (long long) metaData->getVer());

        env->CallVoidMethod(meta, setType, (long long) metaData->getType());

        env->CallVoidMethod(meta, setMeta, (long long) metaData->getMeta());

        if(metaData->getMeta()==2)
        {
            env->CallVoidMethod(meta, setIdSignKey, (long long) metaData->getIdKey());
        }
        else if(metaData->getMeta()==3)
        {
            env->CallVoidMethod(meta, setIdSignKey, (long long) metaData->getIdKey());
            env->CallVoidMethod(meta, setIdEncryptKey, (long long) metaData->getAttach()->getIdKey());
        }
        else if(metaData->getMeta()==1)
        {
            env->CallVoidMethod(meta, setIdEncryptKey, (long long) metaData->getIdKey());
        }


        return meta;

    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_y_encrypt_YEncrypt_init(JNIEnv *env, jclass type, jbyteArray seed_)
{
    try
    {
        jbyte *seed = env->GetByteArrayElements(seed_, NULL);
        const jsize length = env->GetArrayLength(seed_);

        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte* )seed, length);
        YSecurity::CYSecurityFacad::init(data);

        env->ReleaseByteArrayElements(seed_, seed, 0);
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_y_encrypt_YEncrypt_addRandom(JNIEnv *env, jclass type, jbyteArray randomByte_)
{
    try
    {
        jbyte *randomByte = env->GetByteArrayElements(randomByte_, NULL);
        const jsize length = env->GetArrayLength(randomByte_);

        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte* )randomByte_, length);
        YSecurity::CYSecurityFacad::addRandom(data);
        env->ReleaseByteArrayElements(randomByte_, randomByte, 0);

    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_y_encrypt_YEncrypt_decrypKeysMsg(JNIEnv *env, jobject instance, jbyteArray keyMsg_) {

    try
    {
        jbyte *keyMsg = env->GetByteArrayElements(keyMsg_, NULL);
        const jsize length = env->GetArrayLength(keyMsg_);
        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)keyMsg, length);
        env->ReleaseByteArrayElements(keyMsg_, keyMsg, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env, instance);
        auto idMasterPassword = getMasterPasswordId(env, instance);
        YSecurity::CYData<YSecurity::ubyte > key = getPrivateEncryptKeyToReceive(env,instance);
        YSecurity::CYData<YSecurity::ubyte > keySign = getPublicSignKeyToReceive(env,instance);

        YSecurity::CYKey keyEnc (key.getData(), key.getLenData());
        YSecurity::CYKey keySig(keySign.getData(), keySign.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPassword);
        facad.setPublicSignKeyToReceive(&keySig);
        facad.setPrivateEncryptKeyToReceive(&keyEnc);
        auto keys = facad.decrypKeysMsg(data);

        jobject  decrypteMsg = nullptr;

        if(keys)
        {
            auto typeKey = keys->getType();
            auto idWrapper = keys->getIdWrapperKey();
            auto keyData = converCDataMassToJavaArray(env, *keys->getBytesKeyData());
            auto idKey = keys->getId();
            auto date = keys->getDate();
            auto timeLife = keys->getTimelife();

            jclass classKey = env->FindClass("y/encrypt/Key");

            jmethodID setId = env->GetMethodID(classKey, "setId","(J)V");

            jmethodID setIdWrappper = env->GetMethodID(classKey, "setIdWrapper","(J)V");

            jmethodID setType = env->GetMethodID(classKey, "setType","(I)V");

            jmethodID setKey = env->GetMethodID(classKey, "setKey","([B)V");

            jmethodID setDate = env->GetMethodID(classKey, "setDate","(J)V");

            jmethodID setTiemLife = env->GetMethodID(classKey, "setTimeLife","(J)V");

            decrypteMsg = env->AllocObject(classKey);

            env->CallVoidMethod(decrypteMsg, setId, (long long)idKey);
            env->CallVoidMethod(decrypteMsg, setKey, keyData);
            env->CallVoidMethod(decrypteMsg, setDate, (long long) date);
            env->CallVoidMethod(decrypteMsg, setTiemLife, (long long) timeLife);
            env->CallVoidMethod(decrypteMsg, setIdWrappper, (long long) idWrapper);
            env->CallVoidMethod(decrypteMsg, setType, (long long) typeKey);
        }

        return decrypteMsg;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_getHashKey(JNIEnv *env, jobject instance, jlong version, jbyteArray key__) {

    try {
        jbyte *key_ = env->GetByteArrayElements(key__, NULL);
        const jsize length = env->GetArrayLength(key__);
        YSecurity::CYData<YSecurity::ubyte> data((YSecurity::ubyte*)key_, length);
        env->ReleaseByteArrayElements(key__, key_, 0);

        YSecurity::CYData<YSecurity::char_t> pass = getPassword(env, instance);
        auto idMasterPassword = getMasterPasswordId(env, instance);
        YSecurity::CYKey key (data.getData(), data.getLenData());

        YSecurity::CYSecurityFacad facad(pass, idMasterPassword);
        auto res = facad.getHashKey(&key);

        jbyteArray hashKey = converCDataMassToJavaArray(env, res);
        return hashKey;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_EncryptData(JNIEnv *env, jclass clazz, jlong version, jbyteArray data, jbyteArray key) {
    try {
        jbyte *key_ = env->GetByteArrayElements(key, NULL);
        const jsize length = env->GetArrayLength(key);
        YSecurity::CYKey keyEnc ((YSecurity::ubyte*)key_, length);
        env->ReleaseByteArrayElements(key, key_, 0);

        jbyte * data_ = env->GetByteArrayElements(data, NULL);
        const jsize lengthData = env->GetArrayLength(data);
        YSecurity::CYData<YSecurity::ubyte> openData((YSecurity::ubyte*)data_, lengthData);
        env->ReleaseByteArrayElements(data, data_, 0);

        auto res = YSecurity::CYSecurityFacad::encryptData(1, openData, &keyEnc);
        std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>> ptrRes = std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>>(res);
        jbyteArray encryptedData = converCDataMassToJavaArray(env, *res);

        return encryptedData;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_GenerateKey(JNIEnv *env, jclass clazz, jlong version, jbyteArray password, jbyteArray salt,
                                    jlong id) {
    try {

        jbyte * password_ = env->GetByteArrayElements(password, NULL);
        const jsize lengthPassword = env->GetArrayLength(password);
        YSecurity::CYData<YSecurity::char_t> pass((YSecurity::char_t*)password_, lengthPassword);
        env->ReleaseByteArrayElements(password, password_, 0);

        jbyte * salt_ = env->GetByteArrayElements(salt, NULL);
        const jsize lengthsalt = env->GetArrayLength(salt);
        YSecurity::CYData<YSecurity::ubyte> salt__((YSecurity::ubyte*)salt_, lengthsalt);
        env->ReleaseByteArrayElements(salt, salt_, 0);


        auto cKey = YSecurity::CYSecurityFacad::generateSymmetricKeyFromPassword(&pass, &salt__, (YSecurity::yBigUint)id);

        std::unique_ptr<YSecurity::CYKey> ptrCKey = std::unique_ptr<YSecurity::CYKey>(cKey);

        auto keyDataBytes = cKey->getBytesKeyData();

        std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>> ptrKey = std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>>(keyDataBytes);

        jbyteArray key = converCDataMassToJavaArray(env, *keyDataBytes);

        return key;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_y_encrypt_YEncrypt_DecryptData(JNIEnv *env, jclass clazz, jlong version, jbyteArray data, jbyteArray key) {
    try {
        jbyte *key_ = env->GetByteArrayElements(key, NULL);
        const jsize length = env->GetArrayLength(key);
        YSecurity::CYKey keyEnc ((YSecurity::ubyte*)key_, length);
        env->ReleaseByteArrayElements(key, key_, 0);

        jbyte * data_ = env->GetByteArrayElements(data, NULL);
        const jsize lengthData = env->GetArrayLength(data);
        YSecurity::CYData<YSecurity::ubyte> encData((YSecurity::ubyte*)data_, lengthData);
        env->ReleaseByteArrayElements(data, data_, 0);

        auto res = YSecurity::CYSecurityFacad::decryptData(1, encData, &keyEnc);
        std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>> ptrRes = std::unique_ptr<YSecurity::CYData<YSecurity::ubyte>>(res);
        jbyteArray decryptedData = converCDataMassToJavaArray(env, *res);

        return decryptedData;
    }
    catch (YSecurity::CYBaseException const &e)
    {
        ThrowJavaExeption visitor(env);
        e.accept( visitor );
    }
    catch (std::exception &e) {
        throwException(env, e.what());
    }
    return nullptr;
}