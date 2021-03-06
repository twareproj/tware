#include <cstdint>
#include <iostream>
#include <string>

#include "message/JobMessage.pb.h"
#include "net/ZmqSocket.h"
#include "util/Defaults.h"

using namespace std;
using namespace tupleware;

int main(int argc, const char *argv[]) {
    int exec = atoi(argv[1]);
    int iter = atoi(argv[2]);
    int attr = atoi(argv[3]);
    int cent = atoi(argv[4]);
    uint64_t fileId = stoull(argv[5]);
    string addr = argv[6];

    JobMessage *jobMsg = new JobMessage();
    jobMsg->set_stored_proc(2);
    jobMsg->set_exec(exec);
    jobMsg->set_iter(iter);
    jobMsg->set_attr(attr);
    jobMsg->set_cent(cent);
    jobMsg->set_file_id(fileId);
    jobMsg->set_master(addr);
    Socket *jobSock = new ZmqSocket("localhost", Defaults::JOB_PORT, ZMQ_PUSH);
    jobSock->connect();
    jobSock->send(jobMsg);
    usleep(100000);
    return 0;
}
