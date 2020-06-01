#include "SymmetricEncrypt.h"
#include "CYException.h"

namespace YSecurity
{
	CYSymmetricOpenSSLEncypt::CYSymmetricOpenSSLEncypt(const char *  alogitm)
	{
		cipher = EVP_get_cipherbyname(alogitm);

		if (!cipher)
		{
			throw CYAlogitmExistedException();
		}
	}

	CYSymmetricOpenSSLEncypt::CYSymmetricOpenSSLEncypt()
	{
		cipher = EVP_aes_256_cbc();
	}

	CYSymmetricOpenSSLEncypt::~CYSymmetricOpenSSLEncypt()
	{
	}

	bool CYSymmetricOpenSSLEncypt::encryptData(unsigned char *  openData, unsigned int lenOpen, unsigned char * cipherData, unsigned int & lenCipher, unsigned char * key, unsigned char * iv)
	{
		EVP_CIPHER_CTX *ctx;

		int len;

		int ciphertext_len;

		if (!(ctx = EVP_CIPHER_CTX_new()))
		{
			EVP_CIPHER_CTX_free(ctx);
			return false;
		}

		if (1 != EVP_EncryptInit_ex(ctx, cipher, NULL, key, iv))
		{
			EVP_CIPHER_CTX_free(ctx);
			return false;
		}

		if (1 != EVP_EncryptUpdate(ctx, cipherData, &len, openData, lenOpen))
		{
			EVP_CIPHER_CTX_free(ctx);
			return false;
		}
		ciphertext_len = len;

		if (1 != EVP_EncryptFinal_ex(ctx, cipherData + len, &len))
		{
			EVP_CIPHER_CTX_free(ctx);
			return false;
		}


		ciphertext_len += len;

		lenCipher = ciphertext_len;

		EVP_CIPHER_CTX_free(ctx);

		/*unsigned char * data =new unsigned char[lenOpen*10];
		unsigned int openlen = lenOpen*10;
		unsigned int cipherlen = ciphertext_len;

		decryptData(cipherData, cipherlen, data, openlen, key, iv);*/

		return true;
	}

	bool CYSymmetricOpenSSLEncypt::decryptData(unsigned char * cipherData, unsigned int lenCipher, unsigned char *  openData, unsigned int & lenOpen, unsigned char * key, unsigned char * iv)
	{
		EVP_CIPHER_CTX *ctx;

		int len;

		int plaintext_len;

		if (!(ctx = EVP_CIPHER_CTX_new()))
		{
			EVP_CIPHER_CTX_free(ctx);
			throw CYDecryptException("decrypt key 1");
			return false;
		}

		if (1 != EVP_DecryptInit_ex(ctx, cipher, NULL, key, iv))
		{
			EVP_CIPHER_CTX_free(ctx);
			throw CYDecryptException("decrypt key 2");
			return false;
		}

		if (1 != EVP_DecryptUpdate(ctx, openData, &len, cipherData, lenCipher))
		{
			EVP_CIPHER_CTX_free(ctx);
			throw CYDecryptException("decrypt key 3");
			return false;
		}
		plaintext_len = len;

		if (1 != EVP_DecryptFinal_ex(ctx, openData + len, &len))
		{
			EVP_CIPHER_CTX_free(ctx);
			throw CYDecryptException("decrypt key 4");
			return false;
		}
		plaintext_len += len;

		lenOpen = plaintext_len;

		EVP_CIPHER_CTX_free(ctx);
		return true;
	}
	int CYSymmetricOpenSSLEncypt::getBlockSize()
	{
		return EVP_CIPHER_block_size(this->cipher);
	}
	int CYSymmetricOpenSSLEncypt::getLenKey()
	{
		return EVP_CIPHER_key_length(this->cipher);
	}
	int CYSymmetricOpenSSLEncypt::getLenIV()
	{
		return EVP_CIPHER_iv_length(this->cipher);
	}
}
