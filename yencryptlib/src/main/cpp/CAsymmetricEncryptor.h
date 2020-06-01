#pragma once
#include "CEncryptor.h"
namespace YSecurity
{
	class CAsymmetricEncryptor :
		public CEncryptor
	{
	public:
		CAsymmetricEncryptor();
		virtual ~CAsymmetricEncryptor();
		virtual void setAsymmetricKey(unsigned char * key, int lenKey) = 0;
	};
}