#pragma once
#include <openssl/evp.h>

namespace YSecurity
{
	class CYSymmetricOpenSSLEncypt//EVP_aes_256_cbc()
	{
		const EVP_CIPHER *cipher;

	public:
		CYSymmetricOpenSSLEncypt(const char *  alogitm);
		CYSymmetricOpenSSLEncypt();
		~CYSymmetricOpenSSLEncypt();

		bool encryptData(unsigned char *  openData, unsigned int lenOpen, unsigned char * cipherData, unsigned int & lenCipher, unsigned char * key, unsigned char * iv);

		bool decryptData(unsigned char * cipherData, unsigned int lenCipher, unsigned char * openData, unsigned int &lenOpen, unsigned char * key, unsigned char * iv);

		int getBlockSize();

		int getLenKey();

		int getLenIV();
	};
}