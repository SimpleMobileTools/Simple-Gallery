//
// Created by dburc on 9/3/2021.
//
#ifndef ANDROIDLIBRAW_LIBRAW_FD_DATASTREAM_H
#define ANDROIDLIBRAW_LIBRAW_FD_DATASTREAM_H

#include <unistd.h>
#include <libraw/libraw.h>
#include <sys/stat.h>

class LibRaw_fd_datastream : public LibRaw_abstract_datastream {
public:
    LibRaw_fd_datastream(const int aFd);
    virtual ~LibRaw_fd_datastream();
    virtual int valid();
#ifdef LIBRAW_OLD_VIDEO_SUPPORT
    virtual void *make_jas_stream();
#endif
    virtual int read(void *ptr, size_t sz, size_t nmemb);
    virtual int eof();
    virtual int seek(INT64 o, int whence);
    virtual INT64 tell();
    virtual INT64 size() { return _fsize; }
    virtual char *gets(char *s, int sz);
    virtual int scanf_one(const char *fmt, void *val);
    virtual int get_char()
    {
#ifndef LIBRAW_WIN32_CALLS
        return getc_unlocked(f);
#else
        return fgetc(f);
#endif
    }
    virtual const char *fname();
private:
    FILE *f;
    std::string filename;
    INT64 _fsize;
};

#endif //ANDROIDLIBRAW_LIBRAW_FD_DATASTREAM_H
