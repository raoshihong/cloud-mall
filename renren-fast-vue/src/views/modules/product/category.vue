<template>
  <div>
    <el-tree
      :data="menus"
      :props="defaultProps"
      show-checkbox
      :expand-on-click-node="false"
      node-key="catId"
      :default-expanded-keys="expandedKeys"
    >
      <!-- 这里的slot-scope采用了结构的方式,定义了两个变量node和data -->
      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            type="text"
            v-if="node.level <= 2"
            size="mini"
            @click="() => append(data)"
          >
            Append
          </el-button>
          <el-button
            type="text"
            v-if="data.children.length == 0"
            size="mini"
            @click="() => remove(node, data)"
          >
            Delete
          </el-button>
        </span>
      </span>
    </el-tree>
  </div>
</template>

<script>
export default {
  data() {
    return {
      menus: [],
      expandedKeys: [], // 因为每个节点绑定了唯一的nodeKey,所以这块的展开的key为nodeKey绑定的id指
      // 使用pros属性指定映射的属性字段
      defaultProps: {
        children: "children",
        label: "name",
      },
    };
  },
  created() {
    this.getMenus();
  },
  methods: {
    getMenus() {
      this.$http({
        url: this.$http.adornUrl("/product/category/tree"),
        method: "get",
      }).then(({ data }) => {
        // 这里采用{}结构的方式
        console.log(data);
        this.menus = data.data;
      });
    },
    append(data) {
      console.log(data);
    },

    remove(node, data) {
      console.log(node, data);
      this.$confirm(`确定要删除[${data.name}]分类?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          let ids = [data.catId];
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false),
          }).then(({ data }) => {
            this.$message({
              type: "success",
              message: "删除成功!",
            });

            // 重新获取菜单数据
            this.getMenus();

            // 设置默认展开节点
            this.expandedKeys = [node.parent.data.catId];
          });
        })
        .catch(() => {
          this.$message({
            type: "info",
            message: "已取消删除",
          });
        });
    },
  },
};
</script>