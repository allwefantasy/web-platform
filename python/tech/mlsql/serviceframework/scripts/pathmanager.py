import os


class PathManager(object):
    def __init__(self, project_name):
        self.project_name = project_name

    def lib_classes(self):
        return os.path.join(".", "{}-lib".format(self.project_name), "target", "classes")

    def bin_classes(self):
        return os.path.join(".", "{}-bin".format(self.project_name), "target", "classes")
