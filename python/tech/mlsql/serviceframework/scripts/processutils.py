import collections
import errno
import os
import socket
import tempfile

from watchdog.events import FileSystemEventHandler


def _get_unused_port():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(("", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def _make_inc_temp(suffix="", prefix="", directory_name="/tmp/ray"):
    """Return a incremental temporary file name. The file is not created.

    Args:
        suffix (str): The suffix of the temp file.
        prefix (str): The prefix of the temp file.
        directory_name (str) : The base directory of the temp file.

    Returns:
        A string of file name. If there existing a file having
            the same name, the returned name will look like
            "{directory_name}/{prefix}.{unique_index}{suffix}"
    """
    directory_name = os.path.expanduser(directory_name)
    _incremental_dict = collections.defaultdict(lambda: 0)
    index = _incremental_dict[suffix, prefix, directory_name]
    # `tempfile.TMP_MAX` could be extremely large,
    # so using `range` in Python2.x should be avoided.
    while index < tempfile.TMP_MAX:
        if index == 0:
            filename = os.path.join(directory_name, prefix + suffix)
        else:
            filename = os.path.join(directory_name,
                                    prefix + "." + str(index) + suffix)
        index += 1
        if not os.path.exists(filename):
            # Save the index.
            _incremental_dict[suffix, prefix, directory_name] = index
            return filename

    raise FileExistsError(errno.EEXIST,
                          "No usable temporary filename found")


def monitor_dir(path, func):
    from watchdog.observers import Observer
    observer = Observer()
    observer.schedule(CustomEventHandler(func), path, recursive=True)
    observer.start()


class CustomEventHandler(FileSystemEventHandler):
    def __init__(self, func):
        super()
        self.func = func

    def on_event(self, event):
        self.func(event)

    def on_moved(self, event):
        super(CustomEventHandler, self).on_moved(event)
        self.on_event(event)

    def on_created(self, event):
        super(CustomEventHandler, self).on_created(event)

        self.on_event(event)

    def on_deleted(self, event):
        super(CustomEventHandler, self).on_deleted(event)

        self.on_event(event)

    def on_modified(self, event):
        super(CustomEventHandler, self).on_modified(event)

        self.on_event(event)


def new_log_files(name, redirect_output=True):
    """Generate partially randomized filenames for log files.

    Args:
        name (str): descriptive string for this log file.
        redirect_output (bool): True if files should be generated for
            logging stdout and stderr and false if stdout and stderr
            should not be redirected.
            If it is None, it will use the "redirect_output" Ray parameter.

    Returns:
        If redirect_output is true, this will return a tuple of two
            file handles. The first is for redirecting stdout and the
            second is for redirecting stderr.
            If redirect_output is false, this will return a tuple
            of two None objects.
    """
    if not os.path.exists("./logs"):
        os.mkdir("./logs")
    log_stdout = _make_inc_temp(
        suffix=".out", prefix=name, directory_name="./logs")
    log_stderr = _make_inc_temp(
        suffix=".err", prefix=name, directory_name="./logs")
    # Line-buffer the output (mode 1).
    log_stdout_file = open(log_stdout, "a", buffering=1)
    log_stderr_file = open(log_stderr, "a", buffering=1)
    return log_stdout_file, log_stderr_file
