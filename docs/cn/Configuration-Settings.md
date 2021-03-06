---
layout: global
title: 配置项设置
group: Features
priority: 1
---

* Table of Contents
{:toc}

Tachyon有两种类型的配置参数：

1. [配置属性](#configuration-properties)用来配置Tachyon系统运行时的设置;
2. [系统环境属性](#system-environment-properties)用来控制运行Tachyon的Java VM的选项以及一些基本设置。

# 配置属性 {#configuration-properties}

Tachyon启动时会加载默认（也可以指定）配置属性文件从而设置配置属性。

1. Tachyon的配置属性默认值在`tachyon-default.properties`文件中，该文件在Tachyon源代码或者二进制包里都能找到，不建议初学者修改该文件。

2. 每个部署点以及应用客户端都能够通过`tachyon-site.properties`文件将默认属性值覆盖掉，注意该文件必须在Tachyon Java VM的**classpath**中，最简单的方法是将该属性文件放在`$TACHYON_HOME/conf`目录中。

所有Tachyon配置属性都属于以下五类之一：
[一般配置项](#common-configuration)（由Master和Worker共享），
[Master配置项](#master-configuration)，[Worker配置项](#worker-configuration)，
[用户配置项](#user-configuration)以及[集群管理配置项](#cluster-management)（用于在诸如Mesos和YARN的集群管理器上运行Tachyon）。

## 一般配置项 {#common-configuration}

一般配置项包含了不同组件共享的常量。

<table class="table table-striped">
<tr><th>属性名</th><th>默认值</th><th>意义</th></tr>
{% for item in site.data.table.common-configuration %}
  <tr>
    <td>{{ item.propertyName }}</td>
    <td>{{ item.defaultValue }}</td>
    <td>{{ site.data.table.cn.common-configuration.[item.propertyName] }}</td>
  </tr>
{% endfor %}
</table>

## Master配置项 {#master-configuration}

Master配置项指定master节点的信息，例如地址和端口号。

<table class="table table-striped">
<tr><th>属性名</th><th>默认值</th><th>意义</th></tr>
{% for item in site.data.table.master-configuration %}
  <tr>
    <td>{{ item.propertyName }}</td>
    <td>{{ item.defaultValue }}</td>
    <td>{{ site.data.table.cn.master-configuration.[item.propertyName] }}</td>
  </tr>
{% endfor %}
</table>

## Worker配置项 {#worker-configuration}

Worker配置项指定worker节点的信息，例如地址和端口号。

<table class="table table-striped">
<tr><th>属性名</th><th>默认值</th><th>意义</th></tr>
{% for item in site.data.table.worker-configuration %}
  <tr>
    <td>{{ item.propertyName }}</td>
    <td>{{ item.defaultValue }}</td>
    <td>{{ site.data.table.cn.worker-configuration.[item.propertyName] }}</td>
  </tr>
{% endfor %}
</table>


## 用户配置项 {#user-configuration}

用户配置项指定了文件系统访问的相关信息。

<table class="table table-striped">
<tr><th>属性名</th><th>默认值</th><th>意义</th></tr>
{% for item in site.data.table.user-configuration %}
  <tr>
    <td>{{ item.propertyName }}</td>
    <td>{{ item.defaultValue }}</td>
    <td>{{ site.data.table.cn.user-configuration.[item.propertyName] }}</td>
  </tr>
{% endfor %}
</table>

## 集群管理配置项 {#cluster-management}

如果使用诸如Mesos和YARN的集群管理器运行Tachyon，还有额外的配置项。

<table class="table table-striped">
<tr><th>属性名</th><th>默认值</th><th>意义</th></tr>
{% for item in site.data.table.cluster-management %}
  <tr>
    <td>{{ item.propertyName }}</td>
    <td>{{ item.defaultValue }}</td>
    <td>{{ site.data.table.cn.cluster-management.[item.propertyName] }}</td>
  </tr>
{% endfor %}
</table>

## 配置多宿主网络 {#configure-multihomed-networks}

Tachyon提供了一种使用多宿主网络的方式。如果你有多个NIC，并且想让Tachyon master监听所有的NIC，那么你可以将`tachyon.master.bind.host`设置为`0.0.0.0`，这样Tachyon client就可以通过任何一个NIC访问到master。其他以`bind.host`结尾的配置项也是类似的。

# 系统环境属性 {#system-environment-properties}

要运行Tachyon，还需要配置一些系统环境变量，默认情况下，这些变量在`conf/tachyon-env.sh`文件中被定义，如果该文件不存在，你可以从源代码文件夹中的template文件复制得到：

{% include Configuration-Settings/copy-tachyon-env.md %}

有许多频繁用到的Tachyon配置项可以通过环境变量设置，可以通过Shell设置或者在`conf/tachyon-env.sh`文件中修改其默认值。

* `$TACHYON_MASTER_ADDRESS`: Tachyon master地址，默认为localhost。
* `$TACHYON_UNDERFS_ADDRESS`: 底层文件系统地址，默认为`${TACHYON_HOME}/underFSStorage`，即本地文件系统。
* `$TACHYON_JAVA_OPTS`: 针对Master和Workers的Java VM选项。
* `$TACHYON_MASTER_JAVA_OPTS`: 针对Master配置的额外Java VM选项。
* `$TACHYON_WORKER_JAVA_OPTS`: 针对Worker配置的额外Java VM选项，注意，默认情况下，`TACHYON_JAVA_OPTS`被包含在`TACHYON_MASTER_JAVA_OPTS`和`TACHYON_WORKER_JAVA_OPTS`中。

例如，如果你需要将Tachyon与本地的HDFS相连接，并在7001端口启用Java远程调试，可以使用以下命令：

{% include Configuration-Settings/more-conf.md %}
