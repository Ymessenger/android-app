#pragma once
#include "YSecurityTypes.h"
#include "SymmetricEncrypt.h"
namespace YSecurity
{
	class WrapperSymmetricEncryptor
	{
		CYSymmetricOpenSSLEncypt * encryptor;
	public:
		WrapperSymmetricEncryptor();
		yBytes * encryptData(CYKey * key, yBytes * data);
		yBytes * decryptData(CYKey * key, yBytes * data);
		~WrapperSymmetricEncryptor();
	};
}
