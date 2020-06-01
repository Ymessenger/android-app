#pragma once
#include <exception>

namespace YSecurity
{
    class CYBaseException;
	class CYException;
	class CYInitializationException;
	class CYPasswordException;
	class CYAlogitmExistedException;
    class CYRandomStatusException;
	class CYGenerateKeyException;
	class CYKeyException;
	class CYEncryptException;
	class CYDecryptException;
	class CYNoKeyException;
	class CYDataException;
	class CYSignException;
	class CYVerifityException;
	class CYTimeLife;
	class CYTimeLifeMsg;

	struct IVisitor
	{
		virtual ~IVisitor() {}

		virtual void visit( CYException const & _exception ) = 0;
		virtual void visit( CYInitializationException const & _exception ) = 0;
		virtual void visit( CYPasswordException const & _exception ) = 0;
		virtual void visit( CYAlogitmExistedException const & _exception ) = 0;
		virtual void visit( CYRandomStatusException const & _exception ) = 0;
		virtual void visit( CYGenerateKeyException const & _exception ) = 0;
		virtual void visit( CYKeyException const & _exception ) = 0;
		virtual void visit( CYEncryptException const & _exception ) = 0;
		virtual void visit( CYDecryptException const & _exception ) = 0;
		virtual void visit( CYNoKeyException const & _exception ) = 0;
		virtual void visit( CYDataException const & _exception ) = 0;
		virtual void visit( CYSignException const & _exception ) = 0;
		virtual void visit( CYVerifityException const & _exception ) = 0;
		virtual void visit(CYTimeLife const & _exception) = 0;
		virtual void visit(CYTimeLifeMsg const & _exception) = 0;
	};

	class CYBaseException : public std::exception
	{
	public:
		CYBaseException(){}
		virtual void accept( IVisitor & _visitor ) const throw()= 0;
	};

	class CYException : public CYBaseException
	{
		char * _Message;
	public:
		CYException(char const* const _Message="");
		virtual ~CYException();

		virtual const char* what() const noexcept override
		{
			return _Message;
		}

		void  accept( IVisitor & _visitor )  const  throw() override
		{
			_visitor.visit( *this );
		}
	};

	class CYInitializationException : public CYException
	{
	public:
		CYInitializationException();
		~CYInitializationException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYPasswordException : public CYException
	{
	public:
		CYPasswordException();
		~CYPasswordException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYTimeLife : public CYException
	{
	public:
		CYTimeLife(char const* const _Message);
		~CYTimeLife();
		void accept(IVisitor & _visitor) const throw()
		{
			_visitor.visit(*this);
		}
	};

	class CYTimeLifeMsg : public CYException
	{
	public:
		CYTimeLifeMsg(char const* const _Message);
		~CYTimeLifeMsg();
		void accept(IVisitor & _visitor) const throw()
		{
			_visitor.visit(*this);
		}
	};

	class CYAlogitmExistedException : public CYException
	{
	public:
		CYAlogitmExistedException();
		~CYAlogitmExistedException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYRandomStatusException : public CYException
	{
	public:
		CYRandomStatusException();
		~CYRandomStatusException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYGenerateKeyException : public CYException
	{
	public:
		CYGenerateKeyException(char const* const _Message);
		~CYGenerateKeyException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYKeyException : public CYException
	{
	public:
		CYKeyException(char const* const _Message);
		~CYKeyException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYEncryptException : public CYException
	{
	public:
		CYEncryptException(char const* const _Message);
		~CYEncryptException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYDecryptException : public CYException
	{
	public:
		CYDecryptException(char const* const _Message);
		~CYDecryptException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYNoKeyException : public CYException
	{
	public:
		CYNoKeyException(char const* const _Message);
		~CYNoKeyException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};
	   
	class CYDataException : public CYException
	{
	public:
		CYDataException(char const* const _Message);
		~CYDataException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYSignException : public CYException
	{
	public:
		CYSignException(char const* const _Message);
		~CYSignException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};

	class CYVerifityException : public CYException
	{
	public:
		CYVerifityException(char const* const _Message);
		~CYVerifityException();
        void accept( IVisitor & _visitor ) const throw()
        {
            _visitor.visit( *this );
        }
	};
}

