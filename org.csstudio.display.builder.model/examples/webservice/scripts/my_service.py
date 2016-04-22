"""Example for library that queries a site-specific web service
"""
import re
import os
import urllib

# Hack for jython's urllib, see https://github.com/PythonScanClient/PyScanClient/issues/18
if os.name == 'java':
    import sys, _socket

    def checkSocketLib():
        # Workaround: Detect closed NIO_GROUP and ee-create it
        try:
            if _socket.NIO_GROUP.isShutdown():
                _socket.NIO_GROUP = _socket.NioEventLoopGroup(2, _socket.DaemonThreadFactory("PyScan-Netty-Client-%s"))
                sys.registerCloser(_socket._shutdown_threadpool)
        except AttributeError:
            raise Exception("Jython _socket.py has changed from jython_2.7.0")
else:
    def checkSocketLib():
        # C-Python _socket.py needs no fix
        pass


def read_html():
    checkSocketLib()
    f = urllib.urlopen('http://status.sns.ornl.gov/logbook_titles.jsp')
    return f.read()


def format_html(html):
    pattern = re.compile(".*<td>(.+)</td>.*<a.*>(.+)</a>.*")
    
    text = []
    for line in html.split("\n"):
        match = pattern.match(line)
        if match and len(match.groups()) == 2:
            text.append("%s - %s" % match.groups())
    return "\n".join(text)


if __name__ == "__main__":
    print("== Recent Logbook Entry Titles ==")
    print(format_html(read_html()))
