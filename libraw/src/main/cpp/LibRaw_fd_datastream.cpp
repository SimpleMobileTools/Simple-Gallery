//
// Created by dburc on 9/3/2021.
//
#include <fcntl.h>
#include "LibRaw_fd_datastream.h"

#include <android/log.h>
LibRaw_fd_datastream::LibRaw_fd_datastream(const int fd) {
    f = ::fdopen(fd, "r");

    struct stat fdinfo;
    if (!fstat(fd, &fdinfo)) {
        _fsize = fdinfo.st_size;
    }
}

LibRaw_fd_datastream::~LibRaw_fd_datastream()
{
    if (f)
        fclose(f);
}

int LibRaw_fd_datastream::valid() { return f ? 1 : 0; }

#define LR_BF_CHK()                                                            \
  do                                                                           \
  {                                                                            \
    if (!f)                                                                    \
      throw LIBRAW_EXCEPTION_IO_EOF;                                           \
  } while (0)

int LibRaw_fd_datastream::read(void *ptr, size_t size, size_t nmemb)
{
    LR_BF_CHK();
    return int(fread(ptr, size, nmemb, f));
}

int LibRaw_fd_datastream::eof()
{
    LR_BF_CHK();
    return feof(f);
}

int LibRaw_fd_datastream::seek(INT64 o, int whence)
{
    LR_BF_CHK();
#if defined(_WIN32)
    #ifdef WIN32SECURECALLS
  return _fseeki64(f, o, whence);
#else
  return fseek(f, (long)o, whence);
#endif
#else
    return fseeko(f, o, whence);
#endif
}

INT64 LibRaw_fd_datastream::tell()
{
    LR_BF_CHK();
#if defined(_WIN32)
    #ifdef WIN32SECURECALLS
  return _ftelli64(f);
#else
  return ftell(f);
#endif
#else
    return ftello(f);
#endif
}

char *LibRaw_fd_datastream::gets(char *str, int sz)
{
    if(sz<1) return NULL;
    LR_BF_CHK();
    return fgets(str, sz, f);
}

int LibRaw_fd_datastream::scanf_one(const char *fmt, void *val)
{
    LR_BF_CHK();
    return
#ifndef WIN32SECURECALLS
            fscanf(f, fmt, val)
#else
        fscanf_s(f, fmt, val)
#endif
            ;
}

const char *LibRaw_fd_datastream::fname()
{
    return nullptr;
}

