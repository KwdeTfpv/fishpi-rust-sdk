export default {
    base: '/fishpi-rust-sdk/',
    title: 'FishPi 插件开发指南',
    description: 'FishPi Android 插件系统文档',
    themeConfig: {
        outline: {
            level: 'deep',
            label: '本页目录',
        },
        nav: [
            { text: '指南', link: '/' },
        ],
        sidebar: [
            { text: '快速开始', link: '/#快速开始' },
            { text: '文件头', link: '/#文件头' },
            { text: '全局 API', link: '/#全局-api' },
            {
                text: 'SDK API 参考',
                link: '/#sdk-api-参考',
                collapsed: false,
                items: [
                    { text: '聊天室', link: '/#聊天室' },
                    { text: '红包', link: '/#红包' },
                    { text: '用户', link: '/#用户' },
                    { text: '私聊', link: '/#私聊' },
                    { text: '文章', link: '/#文章' },
                    { text: '表情', link: '/#表情' },
                    { text: '清风明月', link: '/#清风明月' },
                    { text: '通知', link: '/#通知' },
                ],
            },
            { text: '完整示例', link: '/#完整示例红包助手' },
            { text: '插件管理', link: '/#插件管理' },
            { text: '调试', link: '/#调试' },
        ],
        socialLinks: [
            { icon: 'github', link: 'https://github.com/KwdeTfpv/fishpi-rust-sdk' },
        ],
    },
}
