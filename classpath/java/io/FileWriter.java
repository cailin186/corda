/* Copyright (c) 2008-2014, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.io;

public class FileWriter extends Writer {
  private final Writer out;

  private FileWriter(FileOutputStream out) {
    this.out = new OutputStreamWriter(out);
  }

  public FileWriter(FileDescriptor fd) {
    this(new FileOutputStream(fd));
  }

  public FileWriter(String path) throws IOException {
    this(new FileOutputStream(path));
  }

  public FileWriter(File file) throws IOException {
    this(new FileOutputStream(file));
  }
  
  public void write(char[] b, int offset, int length) throws IOException {
    out.write(b, offset, length);
  }

  public void flush() throws IOException {
    out.flush();
  }

  public void close() throws IOException {
    out.close();
  }
}
