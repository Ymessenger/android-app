#include "CYException.h"
#include <string>
#include <string.h>

#pragma warning(disable: 4996)
namespace YSecurity
{
	CYException::CYException(char const* const _Message):CYBaseException()
	{
		this->_Message = new char[strlen(_Message)+1];
		memset(this->_Message,0,strlen(_Message)+1);
		strcpy(this->_Message,_Message);
	}

	CYException::~CYException()
	{
		if(_Message)
			delete [] _Message;
	}

	CYInitializationException::CYInitializationException():CYException("Library not initialized")
	{
	}
	CYInitializationException::~CYInitializationException()
	{
	}

	CYPasswordException::CYPasswordException() : CYException("No password")
	{
	}
	CYPasswordException::~CYPasswordException()
	{
	}

	CYAlogitmExistedException::CYAlogitmExistedException() : CYException("Alogitm not existed")
	{
	}
	CYAlogitmExistedException::~CYAlogitmExistedException()
	{
	}

	CYRandomStatusException::CYRandomStatusException(): CYException("Not enough random")
	{
	}
	CYRandomStatusException::~CYRandomStatusException()
	{
	}


	CYGenerateKeyException::CYGenerateKeyException(char const * const _Message): CYException(_Message)
	{
	}
	CYGenerateKeyException::~CYGenerateKeyException()
	{
	}

	CYKeyException::CYKeyException(char const * const _Message) : CYException(_Message)
	{
	}
	CYKeyException::~CYKeyException()
	{
	}

	CYEncryptException::CYEncryptException(char const * const _Message) : CYException(_Message)
	{

	}

	CYEncryptException::~CYEncryptException()
	{
	}

	CYDecryptException::CYDecryptException(char const * const _Message) : CYException(_Message)
	{

	}

	CYDecryptException::~CYDecryptException()
	{
	}

	CYNoKeyException::CYNoKeyException(char const * const _Message) : CYException(_Message)
	{
	}

	CYNoKeyException::~CYNoKeyException()
	{
	}


	CYDataException::CYDataException(char const* const _Message) : CYException(_Message)
	{

	}

	CYDataException::~CYDataException()
	{
	}

	CYSignException::CYSignException(char const * const _Message) : CYException(_Message)
	{
	}

	CYSignException::~CYSignException()
	{
	}

	CYVerifityException::CYVerifityException(char const * const _Message) : CYException(_Message)
	{
	}

	CYVerifityException::~CYVerifityException()
	{
	}

	CYTimeLife::CYTimeLife(char const * const _Message) : CYException(_Message)
	{
	}

	CYTimeLife::~CYTimeLife()
	{
	}

	CYTimeLifeMsg::CYTimeLifeMsg(char const * const _Message) : CYException(_Message)
	{
	}

	CYTimeLifeMsg::~CYTimeLifeMsg()
	{
	}

}
