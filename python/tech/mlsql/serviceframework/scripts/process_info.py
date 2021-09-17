class ProcessInfo(object):
    inner_process = None
    event_buffer = []

    @staticmethod
    def kill():
        if ProcessInfo.inner_process is not None:
            ProcessInfo.inner_process.kill()
            ProcessInfo.inner_process = None
