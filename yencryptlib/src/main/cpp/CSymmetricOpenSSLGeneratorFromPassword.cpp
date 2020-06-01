#include "CSymmetricOpenSSLGeneratorFromPassword.h"
#include "CYException.h"

namespace YSecurity
{

	CSymmetricOpenSSLGeneratorFromPassword::CSymmetricOpenSSLGeneratorFromPassword(const char * alogitm, int iter)
	{
		this->iter = iter;

		this->digest = EVP_get_digestbyname(alogitm);
		if (!this->digest)
		{
			throw CYAlogitmExistedException();
		}
	}

	CSymmetricOpenSSLGeneratorFromPassword::~CSymmetricOpenSSLGeneratorFromPassword()
	{

	}

	bool CSymmetricOpenSSLGeneratorFromPassword::GenereteKeyWithoutSalt(const unsigned char * password, int lenPass, int lenKey, unsigned char * key)
	{
		return PKCS5_PBKDF2_HMAC((char*)password, lenPass, NULL, 0, this->iter, this->digest, lenKey, key);
	}

	bool CSymmetricOpenSSLGeneratorFromPassword::GenereteKey(const unsigned char * password, int lenPass, const unsigned char * salt, int saltLen, int lenKey, unsigned char * key)
	{
		return PKCS5_PBKDF2_HMAC((char*)password, lenPass, salt, saltLen, this->iter, this->digest, lenKey, key);
	}

	bool CSymmetricOpenSSLGeneratorFromPassword::GenereteKey(const unsigned char * password, int lenPass, int lenKey, unsigned char * key)
	{
		int lenSaltSeed = lenPass / 2;
		unsigned char * salt = new unsigned char[lenKey];

		if (!GenereteKeyWithoutSalt(password, lenSaltSeed, lenKey, salt))
		{
			delete[] salt;
			return false;
		}

		if (!GenereteKey(&password[lenSaltSeed], lenPass - lenSaltSeed, salt, lenSaltSeed, lenKey, key))
		{
			delete[] salt;
			return false;
		}

		delete[] salt;
		return true;
	}
}