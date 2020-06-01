#pragma once
#include "YDigest.h"
#include <openssl/evp.h>
namespace YSecurity
{
	class CYDigestOpenSSL : public CYDigest
	{
		EVP_MD_CTX * mdctx;
		void setCtx(const EVP_MD * md);
	public:
		CYDigestOpenSSL();
		CYDigestOpenSSL(char * hashName);
		void addMsg(void * data, unsigned int lenData);
		void getDigest(void * byffer, unsigned int &lenBuffer);
		unsigned int getMaxSizeBuffer();
		~CYDigestOpenSSL();
	};
}
