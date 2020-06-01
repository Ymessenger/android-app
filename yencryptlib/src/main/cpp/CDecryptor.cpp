#include "CDecryptor.h"

namespace YSecurity
{
	CDecryptor::CDecryptor()
	{
	}


	CDecryptor::~CDecryptor()
	{
		if (outData)
		{
			delete[] outData;
		}
	}

	size_t CDecryptor::getLenDecryptedData()
	{
		if (!this->_isError)
		{
			if (this->outData)
			{
				return this->lenOutData;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found encrypted data";
			}
		}
		return 0;
	}

	unsigned char * CDecryptor::getDecryptedData()
	{
		if (!this->_isError)
		{
			if (this->outData)
			{
				return this->outData;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found encrypted data";
			}
		}
		return nullptr;
	}
}