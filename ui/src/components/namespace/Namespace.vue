<template>
    <top-nav-bar :title="routeInfo.title" :breadcrumb="routeInfo.breadcrumb">
        <template #additional-right>
            <ul v-if="$route.params.tab === 'secrets' && canCreateSecret">
                <li>
                    <el-button :icon="FamilyTree" @click="modalInheritedSecretsVisible = true">
                        {{ $t('secret.inherited') }}
                    </el-button>
                </li>
                <li>
                    <el-button :icon="Plus" type="primary" @click="modalAddSecretVisible = true">
                        {{ $t('secret.add') }}
                    </el-button>
                </li>
            </ul>
            <ul v-if="$route.params.tab === 'kv'">
                <li>
                    <el-button :icon="Plus" type="primary" @click="modalAddKvVisible = true">
                        {{ $t('kv.add') }}
                    </el-button>
                </li>
            </ul>
        </template>
    </top-nav-bar>
    <tabs :route-name="$route.param && $route.param.id ? 'namespaces/update' : ''" :tabs="tabs" id="namespaces" />
</template>

<script setup>
    import TopNavBar from "../layout/TopNavBar.vue";
    import FamilyTree from "vue-material-design-icons/FamilyTree.vue";
    import Plus from "vue-material-design-icons/Plus.vue";
</script>

<script>
    import NamespaceDependenciesWrapper from "./NamespaceDependenciesWrapper.vue";
    import Tabs from "../Tabs.vue";
    import RouteContext from "../../mixins/routeContext";
    import {mapState} from "vuex";
    import permission from "../../models/permission";
    import action from "../../models/action";
    import Overview from "./Overview.vue";
    import NamespaceKV from "./NamespaceKV.vue";
    import NamespaceFlows from "./NamespaceFlows.vue";
    import EditorView from "../inputs/EditorView.vue";

    export default {
        mixins: [RouteContext],
        components: {
            Tabs
        },
        data() {
            return {
                modalAddSecretVisible: false,
                modalInheritedSecretsVisible: false,
                modalBindingsVisible: false,
                modalAddKvVisible: false,
            }
        },
        computed: {
            ...mapState("auth", ["user"]),
            ...mapState("namespace", ["dependencies"]),
            canCreateSecret() {
                return this.$route.params.namespace && this.user &&
                    this.user.isAllowed(permission.SECRET, action.CREATE, this.$route.params.namespace);
            },
            canCreateKv() {
                return this.$route.params.namespace;
            },
            routeInfo() {
                return {
                    title: this.$route.params.namespace || this.$t("namespaces"),
                    breadcrumb: [
                        {
                            label: this.$t("namespaces"),
                            link: {
                                name: "namespaces"
                            }
                        }
                    ]
                };
            },
            tabs() {
                const tabs = [];

                tabs.push(...[
                    {
                        name: undefined,
                        component: Overview,
                        title: this.$t("overview"),
                        query: {
                            id: this.$route.query.id
                        }
                    },
                    {
                        name: "editor",
                        component: EditorView,
                        title: this.$t("editor"),
                        props: {
                            tab: "editor",
                            isNamespace: true,
                            isReadOnly: false,
                        },
                        query: {
                            id: this.$route.query.id
                        }
                    },
                    {
                        name: "flows",
                        component: NamespaceFlows,
                        title: this.$t("flows"),
                        props: {
                            tab: "flows",
                        },
                        query: {
                            id: this.$route.query.id
                        }
                    },                   
                    {
                        name: "edit",
                        component: "",
                        title: this.$t("edit"),
                        props: {
                            tab: "edit",
                        },
                        query: {
                            id: this.$route.query.id
                        },
                        disabled: true,
                        locked: true
                    },
                    {
                        name: "variables",
                        component: "",
                        title: this.$t("variables"),
                        containerClass: "full-container",
                        props: {
                            type: "variables",
                            tab: "variables",
                        },
                        disabled: true,
                        locked: true
                    },
                    {
                        name: "plugin-defaults",
                        component: "",
                        title: this.$t("plugin defaults"),
                        containerClass: "full-container",
                        props: {
                            type: "pluginDefaults",
                            tab: "plugin-defaults",
                        },
                        disabled: true,
                        locked: true
                    },
                    {
                        name: "dependencies",
                        component: NamespaceDependenciesWrapper,
                        title: this.$t("dependencies"),
                        props: {
                            type: "dependencies",
                            tab: "dependencies",
                        },
                        query: {
                            id: this.$route.query.id
                        }
                    },
                    {
                        name: "secrets",
                        component: "",
                        title: this.$t("secret.names"),
                        props: {
                            addSecretModalVisible: this.modalAddSecretVisible,
                            inheritedSecretsModalVisible: this.modalInheritedSecretsVisible
                        },
                        "v-on": {
                            "update:addSecretModalVisible": (value) => {
                                this.modalAddSecretVisible = value
                            },
                            "update:inheritedSecretsModalVisible": (value) => {
                                this.modalInheritedSecretsVisible = value
                            }
                        },
                        disabled: true,
                        locked: true
                    },
                    {
                        name: "audit-logs",
                        component: "",
                        title: this.$t("auditlogs"),
                        props: {
                            restoreUrl: false
                        },
                        disabled: true,
                        locked: true
                    },
                    {
                        name: "kv",
                        component: NamespaceKV,
                        title: this.$t("kv.name"),
                        props: {
                            addKvModalVisible: this.modalAddKvVisible,
                        },
                        "v-on": {
                            "update:addKvModalVisible": (value) => {
                                this.modalAddKvVisible = value
                            }
                        }
                    }
                ])

                return tabs;
            }
        },
        mounted () {
            this.loadItem()
        },
        methods: {
            loadItem() {
                if (this.$route.params.namespace) {
                    this.$store
                        .dispatch(
                            "namespace/load",
                            this.$route.params
                        )
                }
            },
        }
    };
</script>

<style lang="scss">
section#namespaces div {
    &:has(div.namespace-form) {
        display: flex;
    }
}
</style>