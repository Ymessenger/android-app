#pragma once
#include <openssl/evp.h>

namespace YSecurity
{
	class CSymmetricOpenSSLGeneratorFromPassword
	{
		const EVP_MD *digest;
		int iter;

	public:
		CSymmetricOpenSSLGeneratorFromPassword(const char *  alogitm, int iter);
		~CSymmetricOpenSSLGeneratorFromPassword();

		bool GenereteKeyWithoutSalt(const unsigned char * password, int lenPass, int lenKey, unsigned char * key);
		bool GenereteKey(const unsigned char * password, int lenPass, const unsigned char * salt, int saltLen, int lenKey, unsigned char * key);
		bool GenereteKey(const unsigned char * password, int lenPass, int lenKey, unsigned char * key);
	};
}

