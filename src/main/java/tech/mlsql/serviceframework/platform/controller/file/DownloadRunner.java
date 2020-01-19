package tech.mlsql.serviceframework.platform.controller.file;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.kamranzafar.jtar.TarOutputStream;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by allwefantasy on 11/7/2017.
 */
public class DownloadRunner {

    private static Logger logger = Logger.getLogger(DownloadRunner.class);

    public static int getHDFSTarFileByPath(HttpServletResponse res, String pathStr) {
        String[] paths = pathStr.split(",");
        try {
            OutputStream outputStream = res.getOutputStream();

            TarOutputStream tarOutputStream = new TarOutputStream(new BufferedOutputStream(outputStream));

            FileSystem fs = FileSystem.get(new Configuration());
            List<FileStatus> files = new ArrayList<FileStatus>();

            for (String path : paths) {
                Path p = new Path(path);
                if (fs.exists(p)) {
                    if (fs.isFile(p)) {
                        files.add(fs.getFileStatus(p));
                    } else if (fs.isDirectory(p)) {
                        FileStatus[] fileStatusArr = fs.listStatus(p);
                        if (fileStatusArr != null && fileStatusArr.length > 0) {

                            for (FileStatus cur : fileStatusArr) {
                                if (cur.isFile()) {
                                    files.add(cur);
                                }
                            }
                        }
                    }
                }

            }

            if (files.size() > 0) {
                FSDataInputStream inputStream = null;
                int len = files.size();
                int i = 1;
                for (FileStatus cur : files) {
                    logger.info("[" + i++ + "/" + len + "]" + ",读取文件" + cur);
                    inputStream = fs.open(cur.getPath());

                    tarOutputStream.putNextEntry(new HDFSTarEntry(cur, cur.getPath().getName()));
                    org.apache.commons.io.IOUtils.copyLarge(inputStream, tarOutputStream);
                    inputStream.close();

                }
                tarOutputStream.flush();
                tarOutputStream.close();
                return 200;
            } else return 400;

        } catch (Exception e) {
            e.printStackTrace();
            return 500;

        }
    }

    public static int getHDFSRawFileByPath(HttpServletResponse res, String path, long position) {

        try {
            FileSystem fs = FileSystem.get(new Configuration());

            Path p = new Path(path);
            if (fs.exists(p)) {

                List<FileStatus> files = new ArrayList<FileStatus>();

                //找出所有文件
                if (fs.isFile(p)) {
                    files.add(fs.getFileStatus(p));
                } else if (fs.isDirectory(p)) {

                    FileStatus[] fileStatusArr = fs.listStatus(p);
                    if (fileStatusArr != null && fileStatusArr.length > 0) {

                        for (FileStatus cur : fileStatusArr) {
                            files.add(cur);
                        }
                    }
                }


                //遍历所有文件
                if (files.size() > 0) {

                    logger.info(path + "找到文件" + files.size());

                    FSDataInputStream inputStream = null;
                    OutputStream outputStream = res.getOutputStream();

                    int len = files.size();
                    int i = 1;
                    long allPosition = 0;
                    for (FileStatus cur : files) {


                        logger.info("[" + i++ + "/" + len + "]" + path + ",读取文件" + cur);
                        inputStream = fs.open(cur.getPath());


                        if (position > 0) {


                            if (allPosition + cur.getLen() > position) {
                                inputStream.seek(position - allPosition);
                                logger.info("seek position " + (position - allPosition));
                                position = -1;
                            }
                            allPosition += cur.getLen();
                        }
                        org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream);
                        inputStream.close();

                    }
                    outputStream.flush();
                    outputStream.close();
                    return 200;


                } else {
                    logger.info(path + "没有找到文件" + files.size());
                }


            } else {

                return 400;
            }


        } catch (Exception e) {
            e.printStackTrace();

        }

        return 500;
    }


    private static final String HEADER_KEY = "Content-Disposition";
    private static final String HEADER_VALUE = "attachment; filename=";


    public static void iteratorFiles(String path, List<File> files) {
        File p = new File(path);
        if (p.exists()) {
            if (p.isFile()) {
                files.add(p);
            } else if (p.isDirectory()) {
                File[] fileStatusArr = p.listFiles();
                if (fileStatusArr != null && fileStatusArr.length > 0) {
                    for (File file : fileStatusArr) {
                        iteratorFiles(file.getPath(), files);
                    }

                }
            }
        }
    }

    public static int getTarFileByTarFile(HttpServletResponse response, String pathStr) throws UnsupportedEncodingException {

        String[] fileChunk = pathStr.split("/");
        response.setContentType("application/octet-stream");
        //response.setHeader("Transfer-Encoding", "chunked");
        response.setHeader(HEADER_KEY, HEADER_VALUE + "\"" + URLEncoder.encode(fileChunk[fileChunk.length - 1], "utf-8") + "\"");

        try {
            org.apache.commons.io.IOUtils.copyLarge(new FileInputStream(new File(pathStr)), response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return 500;

        }
        return 200;
    }

    public static int getTarFileByPath(HttpServletResponse response, String pathStr) throws UnsupportedEncodingException {

        String[] fileChunk = pathStr.split("/");
        response.setContentType("application/octet-stream");
        //response.setHeader("Transfer-Encoding", "chunked");
        response.setHeader(HEADER_KEY, HEADER_VALUE + "\"" + URLEncoder.encode(fileChunk[fileChunk.length - 1] + ".tar", "utf-8") + "\"");


        try {
            OutputStream outputStream = response.getOutputStream();

            ArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream);

            List<File> files = new ArrayList<File>();

            iteratorFiles(pathStr, files);

            if (files.size() > 0) {
                InputStream inputStream = null;
                int len = files.size();
                int i = 1;
                for (File cur : files) {
                    logger.info("[" + i++ + "/" + len + "]" + ",读取文件" + cur.getPath() + " entryName:" + fileChunk[fileChunk.length - 1] + cur.getPath().substring(pathStr.length()));
                    inputStream = new FileInputStream(cur);
                    ArchiveEntry entry = tarOutputStream.createArchiveEntry(cur, fileChunk[fileChunk.length - 1] + cur.getPath().substring(pathStr.length()));
                    tarOutputStream.putArchiveEntry(entry);
                    org.apache.commons.io.IOUtils.copyLarge(inputStream, tarOutputStream);
                    tarOutputStream.closeArchiveEntry();
                }
                tarOutputStream.flush();
                tarOutputStream.close();
                return 200;
            } else return 400;

        } catch (Exception e) {
            e.printStackTrace();
            return 500;

        }
    }
}
