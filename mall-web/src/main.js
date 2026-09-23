import { createApp } from 'vue'
import Alert from 'ant-design-vue/es/alert'
import Button from 'ant-design-vue/es/button'
import Card from 'ant-design-vue/es/card'
import Checkbox from 'ant-design-vue/es/checkbox'
import Col from 'ant-design-vue/es/col'
import DatePicker from 'ant-design-vue/es/date-picker'
import Descriptions from 'ant-design-vue/es/descriptions'
import Divider from 'ant-design-vue/es/divider'
import Drawer from 'ant-design-vue/es/drawer'
import Dropdown from 'ant-design-vue/es/dropdown'
import Empty from 'ant-design-vue/es/empty'
import Form from 'ant-design-vue/es/form'
import Input from 'ant-design-vue/es/input'
import InputNumber from 'ant-design-vue/es/input-number'
import Layout from 'ant-design-vue/es/layout'
import Menu from 'ant-design-vue/es/menu'
import Modal from 'ant-design-vue/es/modal'
import Popconfirm from 'ant-design-vue/es/popconfirm'
import Radio from 'ant-design-vue/es/radio'
import Result from 'ant-design-vue/es/result'
import Row from 'ant-design-vue/es/row'
import Select from 'ant-design-vue/es/select'
import Space from 'ant-design-vue/es/space'
import Spin from 'ant-design-vue/es/spin'
import Statistic from 'ant-design-vue/es/statistic'
import Switch from 'ant-design-vue/es/switch'
import Table from 'ant-design-vue/es/table'
import Tabs from 'ant-design-vue/es/tabs'
import Tag from 'ant-design-vue/es/tag'
import Tree from 'ant-design-vue/es/tree'
import TreeSelect from 'ant-design-vue/es/tree-select'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import pinia from './store'

const app = createApp(App)
;[
  Alert, Button, Card, Checkbox, Col, DatePicker, Descriptions, Divider, Drawer,
  Dropdown, Empty, Form, Input, InputNumber, Layout, Menu, Modal, Popconfirm,
  Radio, Result, Row, Select, Space, Spin, Statistic, Switch, Table, Tabs, Tag,
  Tree, TreeSelect
].forEach(component => app.use(component))
app.use(router)
app.use(pinia)
app.mount('#app')
